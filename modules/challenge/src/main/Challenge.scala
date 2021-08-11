package lila.challenge

import strategygames.format.FEN
import strategygames.variant.Variant
import strategygames.chess.variant.{ Chess960, FromPosition, Horde, RacingKings, LinesOfAction }
import strategygames.{ Black, Color, GameLib, Mode, Speed, White }
import org.joda.time.DateTime

import lila.game.{ Game, PerfPicker }
import lila.i18n.{ I18nKey, I18nKeys }
import lila.rating.PerfType
import lila.user.User

case class Challenge(
    _id: String,
    status: Challenge.Status,
    variant: Variant,
    initialFen: Option[FEN],
    timeControl: Challenge.TimeControl,
    mode: Mode,
    colorChoice: Challenge.ColorChoice,
    finalColor: Color,
    challenger: Challenge.Challenger,
    destUser: Option[Challenge.Challenger.Registered],
    rematchOf: Option[Game.ID],
    createdAt: DateTime,
    seenAt: Option[DateTime], // None for open challenges, so they don't sweep
    expiresAt: DateTime,
    open: Option[Boolean] = None,
    name: Option[String] = None,
    declineReason: Option[Challenge.DeclineReason] = None,
    microMatch: Option[Boolean] = None
) {

  import Challenge._

  def id = _id

  def challengerUser =
    challenger match {
      case u: Challenger.Registered => u.some
      case _                        => none
    }
  def challengerUserId = challengerUser.map(_.id)
  def challengerIsAnon =
    challenger match {
      case _: Challenger.Anonymous => true
      case _                       => false
    }
  def challengerIsOpen =
    challenger match {
      case Challenger.Open => true
      case _               => false
    }
  def destUserId = destUser.map(_.id)

  def userIds = List(challengerUserId, destUserId).flatten

  def daysPerTurn =
    timeControl match {
      case TimeControl.Correspondence(d) => d.some
      case _                             => none
    }
  def unlimited = timeControl == TimeControl.Unlimited

  def clock =
    timeControl match {
      case c: TimeControl.Clock => c.some
      case _                    => none
    }

  def hasClock = clock.isDefined

  def openDest = destUser.isEmpty
  def online   = status == Status.Created
  def active   = online || status == Status.Offline
  def declined = status == Status.Declined
  def accepted = status == Status.Accepted

  def setChallenger(u: Option[User], secret: Option[String]) =
    copy(
      challenger = u.map(toRegistered(variant, timeControl)) orElse
        secret.map(Challenger.Anonymous.apply) getOrElse Challenger.Open
    )
  def setDestUser(u: User) =
    copy(
      destUser = toRegistered(variant, timeControl)(u).some
    )

  def speed = speedOf(timeControl)

  def notableInitialFen: Option[FEN] =
    variant match {
      case Variant.Chess(variant) => variant match {
        case FromPosition | Horde | RacingKings | Chess960 | LinesOfAction => initialFen
        case _ => none
      }
      case Variant.Draughts(_) => customStartingPosition ?? initialFen
      case _ => none
    }

  def customStartingPosition: Boolean =
    variant.draughtsFromPosition ||
      (draughtsFromPositionVariants(variant) &&
        initialFen.isDefined &&
        !initialFen.exists(_.value == variant.initialFen.value)
      )

  def isOpen = ~open

  def isMicroMatch = ~microMatch

  lazy val perfType = perfTypeOf(variant, timeControl)

  def anyDeclineReason = declineReason | DeclineReason.default

  def declineWith(reason: DeclineReason) = copy(
    status = Status.Declined,
    declineReason = reason.some
  )
}

object Challenge {

  type ID = String

  sealed abstract class Status(val id: Int) {
    val name = toString.toLowerCase
  }
  object Status {
    case object Created  extends Status(10)
    case object Offline  extends Status(15)
    case object Canceled extends Status(20)
    case object Declined extends Status(30)
    case object Accepted extends Status(40)
    val all                            = List(Created, Offline, Canceled, Declined, Accepted)
    def apply(id: Int): Option[Status] = all.find(_.id == id)
  }

  sealed abstract class DeclineReason(val trans: I18nKey) {
    val key = toString.toLowerCase
  }

  object DeclineReason {
    case object Generic     extends DeclineReason(I18nKeys.challenge.declineGeneric)
    case object Later       extends DeclineReason(I18nKeys.challenge.declineLater)
    case object TooFast     extends DeclineReason(I18nKeys.challenge.declineTooFast)
    case object TooSlow     extends DeclineReason(I18nKeys.challenge.declineTooSlow)
    case object TimeControl extends DeclineReason(I18nKeys.challenge.declineTimeControl)
    case object Rated       extends DeclineReason(I18nKeys.challenge.declineRated)
    case object Casual      extends DeclineReason(I18nKeys.challenge.declineCasual)
    case object Standard    extends DeclineReason(I18nKeys.challenge.declineStandard)
    case object Variant     extends DeclineReason(I18nKeys.challenge.declineVariant)
    case object NoBot       extends DeclineReason(I18nKeys.challenge.declineNoBot)
    case object OnlyBot     extends DeclineReason(I18nKeys.challenge.declineOnlyBot)

    val default: DeclineReason = Generic
    val all: List[DeclineReason] =
      List(Generic, Later, TooFast, TooSlow, TimeControl, Rated, Casual, Standard, Variant, NoBot, OnlyBot)
    val allExceptBot: List[DeclineReason] =
      all.filterNot(r => r == NoBot || r == OnlyBot)
    def apply(key: String) = all.find { d => d.key == key.toLowerCase || d.trans.key == key } | Generic
  }

  case class Rating(int: Int, provisional: Boolean) {
    def show = s"$int${if (provisional) "?" else ""}"
  }
  object Rating {
    def apply(p: lila.rating.Perf): Rating = Rating(p.intRating, p.provisional)
  }

  sealed trait Challenger
  object Challenger {
    case class Registered(id: User.ID, rating: Rating) extends Challenger
    case class Anonymous(secret: String)               extends Challenger
    case object Open                                   extends Challenger
  }

  sealed trait TimeControl
  object TimeControl {
    case object Unlimited                extends TimeControl
    case class Correspondence(days: Int) extends TimeControl
    case class Clock(config: strategygames.Clock.Config) extends TimeControl {
      // All durations are expressed in seconds
      def limit     = config.limit
      def increment = config.increment
      def show      = config.show
    }
  }

  sealed trait ColorChoice
  object ColorChoice {
    case object Random extends ColorChoice
    case object White  extends ColorChoice
    case object Black  extends ColorChoice
    def apply(c: Color) = c.fold[ColorChoice](White, Black)
  }

  private def speedOf(timeControl: TimeControl) =
    timeControl match {
      case TimeControl.Clock(config) => Speed(config)
      case _                         => Speed.Correspondence
    }

  private def perfTypeOf(variant: Variant, timeControl: TimeControl): PerfType =
    PerfPicker
      .perfType(
        speedOf(timeControl),
        variant,
        timeControl match {
          case TimeControl.Correspondence(d) => d.some
          case _                             => none
        }
      )
      .orElse {
        (variant == Variant.libFromPosition(variant.gameLib)) option perfTypeOf(Variant.libStandard(variant.gameLib), timeControl)
      }
      .|(PerfType.Correspondence)

  private val idSize = 8

  private def randomId = lila.common.ThreadLocalRandom nextString idSize

  def toRegistered(variant: Variant, timeControl: TimeControl)(u: User) =
    Challenger.Registered(u.id, Rating(u.perfs(perfTypeOf(variant, timeControl))))

  def randomColor = Color.fromWhite(lila.common.ThreadLocalRandom.nextBoolean())

  // NOTE: Only variants with standardInitialPosition = false!
  private val draughtsFromPositionVariants: Set[Variant] = Set(
    strategygames.draughts.variant.FromPosition,
    strategygames.draughts.variant.Russian,
    strategygames.draughts.variant.Brazilian
  ).map(Variant.Draughts)

  def make(
      variant: Variant,
      fenVariant: Option[Variant],
      initialFen: Option[FEN],
      timeControl: TimeControl,
      mode: Mode,
      color: String,
      challenger: Challenger,
      destUser: Option[User],
      rematchOf: Option[Game.ID],
      name: Option[String] = None,
      microMatch: Boolean = false
  ): Challenge = {
    val (colorChoice, finalColor) = color match {
      case "white" => ColorChoice.White  -> White
      case "black" => ColorChoice.Black  -> Black
      case _       => ColorChoice.Random -> randomColor
    }
    val finalVariant = fenVariant match {
      case Some(v) if draughtsFromPositionVariants(variant) =>
        if (variant.draughtsFromPosition && v.draughtsStandard)
          Variant.libFromPosition(GameLib.Draughts())
        else v
      case _ => variant
    }
    //val finalInitialFen = finalVariant match {
    //  case Variant.Draughts(v) =>
    //    draughtsFromPositionVariants(v) ?? {
    //      initialFen.flatMap(fen => Forsyth.<<@(finalVariant.gameLib, finalVariant, fen.value))
    //        .map(sit => FEN(Forsyth.>>(finalVariant.gameLib, sit.withoutGhosts)))
    //    } match {
    //      case fen @ Some(_) => fen
    //      case _ => !finalVariant.standardInitialPosition option FEN(finalVariant.initialFen)
    //    }
    //}
    val finalMode = timeControl match {
      case TimeControl.Clock(clock) if !lila.game.Game.allowRated(variant, clock.some)
        => Mode.Casual
      case _ => mode
    }
    val isOpen = challenger == Challenge.Challenger.Open
    new Challenge(
      _id = randomId,
      status = Status.Created,
      variant = variant,
      initialFen =
        if (variant == Variant.libFromPosition(variant.gameLib)) initialFen
        else if (variant == Variant.Chess(Chess960)) initialFen filter { fen =>
          fen.chessFen.map(fen => Chess960.positionNumber(fen).isDefined).getOrElse(false)
        }
        else !variant.standardInitialPosition option variant.initialFen,
      timeControl = timeControl,
      mode = finalMode,
      colorChoice = colorChoice,
      finalColor = finalColor,
      challenger = challenger,
      destUser = destUser map toRegistered(variant, timeControl),
      rematchOf = rematchOf,
      createdAt = DateTime.now,
      seenAt = !isOpen option DateTime.now,
      expiresAt = if (isOpen) DateTime.now.plusDays(1) else inTwoWeeks,
      open = isOpen option true,
      name = name,
      microMatch = microMatch option true
    )
    //TODO microMatch: is this needed?
    /*) |> { challenge =>
      if (microMatch && !challenge.customStartingPosition)
        challenge.copy(microMatch = none)
      else challenge
    } |> { challenge =>
      if (challenge.mode.rated && !challenge.isMicroMatch && challenge.customStartingPosition)
        challenge.copy(mode = Mode.Casual)
      else challenge
    }*/
  }
}
