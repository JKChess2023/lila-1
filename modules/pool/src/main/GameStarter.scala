package lila.pool

import scala.concurrent.duration._

import strategygames.{ Game => StratGame, P1, P2, Situation, GameLogic }
import strategygames.chess

import lila.game.{ Game, GameRepo, IdGenerator, Player, Source }
import lila.rating.Perf
import lila.user.{ User, UserRepo }

final private class GameStarter(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    idGenerator: IdGenerator,
    onStart: Game.Id => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import PoolApi._

  private val workQueue = new lila.hub.DuctSequencer(maxSize = 32, timeout = 10 seconds, name = "gameStarter")

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit =
    couples.nonEmpty ?? {
      workQueue {
        val userIds = couples.flatMap(_.userIds)
        userRepo.perfOf(userIds, pool.perfType) flatMap { perfs =>
          idGenerator.games(couples.size) flatMap { ids =>
            couples.zip(ids).map((one(pool, perfs) _).tupled).sequenceFu.map { pairings =>
              lila.common.Bus.publish(Pairings(pairings.flatten.toList), "poolPairings")
            }
          }
        }
      }
    }

  private def one(pool: PoolConfig, perfs: Map[User.ID, Perf])(
      couple: MatchMaking.Couple,
      id: Game.ID
  ): Fu[Option[Pairing]] = {
    import couple._
    import cats.implicits._
    (perfs.get(p1.userId), perfs.get(p2.userId)).mapN((_, _)) ?? { case (perf1, perf2) =>
      for {
        p1P1 <- userRepo.firstGetsP1(p1.userId, p2.userId)
        (p1Perf, p2Perf)     = if (p1P1) perf1 -> perf2 else perf2 -> perf1
        (p1Member, p2Member) = if (p1P1) p1 -> p2 else p2 -> p1
        game = makeGame(
          id,
          pool,
          p1Member.userId -> p1Perf,
          p2Member.userId -> p2Perf
        ).start
        _ <- gameRepo insertDenormalized game
      } yield {
        onStart(Game.Id(game.id))
        Pairing(
          game,
          p1Sri = p1Member.sri,
          p2Sri = p2Member.sri
        ).some
      }
    }
  }

  private def makeGame(
      id: Game.ID,
      pool: PoolConfig,
      p1User: (User.ID, Perf),
      p2User: (User.ID, Perf)
  ) =
    Game
      .make(
        chess = strategygames
          .Game(
            pool.variant.gameLogic,
            situation = Situation(pool.variant.gameLogic, pool.variant),
            clock = pool.clock.toClock.some
          ),
        p1Player = Player.make(P1, p1User),
        p2Player = Player.make(P2, p2User),
        mode = strategygames.Mode.Rated,
        source = Source.Pool,
        daysPerTurn = None,
        pgnImport = None
      )
      .withId(id)
}
