package lila.tournament

import strategygames.Clock.{ Config => ClockConfig }
import strategygames.format.FEN
import strategygames.{ GameFamily, Mode, Speed }
import strategygames.variant.Variant
import org.joda.time.{ DateTime, Duration, Interval }
import play.api.i18n.Lang
import scala.util.chaining._

import lila.common.GreatPlayer
import lila.common.ThreadLocalRandom
import lila.i18n.defaultLang
import lila.rating.PerfType
import lila.user.User

case class Tournament(
    id: Tournament.ID,
    name: String,
    status: Status,
    clock: ClockConfig,
    minutes: Int,
    variant: Variant,
    medleyVariants: Option[List[Variant]] = None,
    medleyMinutes: Option[Int] = None,
    position: Option[FEN],
    mode: Mode,
    password: Option[String] = None,
    conditions: Condition.All,
    teamBattle: Option[TeamBattle] = None,
    noBerserk: Boolean = false,
    noStreak: Boolean = false,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    startsAt: DateTime,
    winnerId: Option[User.ID] = None,
    featuredId: Option[String] = None,
    spotlight: Option[Spotlight] = None,
    description: Option[String] = None,
    trophy1st: Option[String] = None,
    trophy2nd: Option[String] = None,
    trophy3rd: Option[String] = None,
    trophyExpiryDays: Option[Int] = None,
    botsAllowed: Boolean = false,
    hasChat: Boolean = true
) {

  def isCreated   = status == Status.Created
  def isStarted   = status == Status.Started
  def isFinished  = status == Status.Finished
  def isEnterable = !isFinished

  def isPrivate = password.isDefined

  def isTeamBattle = teamBattle.isDefined

  def name(full: Boolean = true)(implicit lang: Lang): String = {
    import lila.i18n.I18nKeys.tourname._
    if (isMarathon || isUnique) name
    else if (isTeamBattle && full) xTeamBattle.txt(name)
    else if (isTeamBattle) name
    //else schedule.fold(if (full) s"$name Arena" else name)(_.name(full))
    else if (full) s"$name Arena"
    else name
  }

  def isMarathon =
    schedule.map(_.freq) exists {
      case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon => true
      case _                                                           => false
    }

  def isShield = schedule.map(_.freq) has Schedule.Freq.Shield

  def isUnique = schedule.map(_.freq) has Schedule.Freq.Unique

  def isMarathonOrUnique = isMarathon || isUnique

  def isMSO = (schedule.map(_.freq) has Schedule.Freq.MSO21) || (schedule.map(_.freq) has Schedule.Freq.MSOGP)

  def isMSOWarmUp = schedule.map(_.freq) has Schedule.Freq.MSOWarmUp

  def isIntro = schedule.map(_.freq) has Schedule.Freq.Introductory

  def isPlayStrategyHeadline = isMSO || isMSOWarmUp || isIntro

  def isScheduled = schedule.isDefined

  def isRated = mode == Mode.Rated

  def isMedley = medleyVariants.nonEmpty

  def finishesAt = startsAt plusMinutes minutes

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt atLeast 0

  def pairingsClosedSeconds = math.max(30, math.min(clock.limitSeconds / 2, 120))

  def pairingsClosed = secondsToFinish < pairingsClosedSeconds

  def medleyRound: Option[Int] =
    if (isStarted) medleyMinutes.map(_ * 60).map((nowSeconds - startsAt.getSeconds).toInt / _)
    else None

  def currentVariant =
    medleyVariants.getOrElse(List()).lift(medleyRound.getOrElse(0)).getOrElse(variant)

  def currentPerfType: PerfType = PerfType(currentVariant, speed)

  //essentially take medleyVariants and discard ones not to display
  def medleyRounds: Option[List[Variant]] =
    medleyRoundsWithMinsRemaining.map(_.map { case (_, v) => v })

  //might want to make this public for something at some point
  private def medleyRoundsWithMinsRemaining: Option[List[(Int, Variant)]] =
    medleyMinutes.map(mm =>
      List
        .tabulate((minutes / mm) + 1)(n => minutes - (n * mm))
        .filter(_ > (pairingsClosedSeconds.toDouble / 60))
        .zip(medleyVariants.getOrElse(List()))
    )

  def isStillWorthEntering =
    isPlayStrategyHeadline || isMarathonOrUnique || {
      secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
    }

  def isRecentlyFinished = isFinished && (nowSeconds - finishesAt.getSeconds) < 30 * 60

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.getSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished

  def isDistant = startsAt.isAfter(DateTime.now plusDays 1)

  def duration = new Duration(minutes * 60 * 1000)

  def interval = new Interval(startsAt, duration)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def similarTo(other: Tournament) =
    (schedule, other.schedule) match {
      case (Some(s1), Some(s2)) if s1 similarTo s2 => true
      case _                                       => false
    }

  def speed = Speed(clock)

  def perfType: PerfType = PerfType(variant, speed)

  def iconChar = if (isMedley) '5' else perfType.iconChar

  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${minutes % 60}m" else "")

  def berserkable = !noBerserk && clock.berserkable
  def streakable  = !noStreak

  def clockStatus =
    secondsToFinish pipe { s =>
      "%02d:%02d".format(s / 60, s % 60)
    }

  def schedulePair = schedule map { this -> _ }

  def winner =
    winnerId map { userId =>
      Winner(
        tourId = id,
        userId = userId,
        tourName = name,
        date = finishesAt
      )
    }

  def nonPlayStrategyCreatedBy = (createdBy != User.playstrategyId) option createdBy

  def ratingVariant = if (variant.fromPosition) Variant.libStandard(variant.gameLogic) else variant

  def startingPosition = position flatMap Thematic.byFen

  lazy val looksLikePrize = !isScheduled && lila.common.String.looksLikePrize(s"$name $description")

  def medleyGameFamilies: Option[List[GameFamily]] = medleyVariants.map(
    _.map(_.gameFamily).distinct.sortWith(_.name < _.name)
  )

  def medleyGameFamiliesString: Option[String] =
    medleyGameFamilies.map(_.map(_.name).mkString(", "))

  override def toString = s"$id $startsAt ${name()(defaultLang)} $minutes minutes, $clock, $nbPlayers players"
}

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament {

  type ID = String

  val minPlayers = 2

  def make(
      by: Either[User.ID, User],
      name: Option[String],
      clock: ClockConfig,
      minutes: Int,
      variant: Variant,
      medleyVariants: Option[List[Variant]] = None,
      medleyMinutes: Option[Int] = None,
      position: Option[FEN],
      mode: Mode,
      password: Option[String],
      waitMinutes: Int,
      startDate: Option[DateTime],
      berserkable: Boolean,
      streakable: Boolean,
      teamBattle: Option[TeamBattle],
      description: Option[String],
      hasChat: Boolean
  ) =
    Tournament(
      id = makeId,
      name = name | (position match {
        case Some(pos) => Thematic.byFen(pos).fold("Custom position")(_.shortName)
        case None      => GreatPlayer.randomName
      }),
      status = Status.Created,
      clock = clock,
      minutes = minutes,
      createdBy = by.fold(identity, _.id),
      createdAt = DateTime.now,
      nbPlayers = 0,
      variant = variant,
      medleyVariants = medleyVariants,
      medleyMinutes = medleyMinutes,
      position = position,
      mode = mode,
      password = password,
      conditions = Condition.All.empty,
      teamBattle = teamBattle,
      noBerserk = !berserkable,
      noStreak = !streakable,
      schedule = None,
      startsAt = startDate match {
        case Some(startDate) => startDate plusSeconds ThreadLocalRandom.nextInt(60)
        case None            => DateTime.now plusMinutes waitMinutes
      },
      description = description,
      hasChat = hasChat
    )

  def scheduleAs(sched: Schedule, minutes: Int) =
    Tournament(
      id = makeId,
      name = sched.medleyShield.fold(sched.name(full = false)(defaultLang))(ms =>
        TournamentShield.MedleyShield.makeName(ms.medleyName, sched.at)
      ),
      status = Status.Created,
      clock = Schedule clockFor sched,
      minutes = minutes,
      createdBy = User.playstrategyId,
      createdAt = DateTime.now,
      nbPlayers = 0,
      variant = sched.variant,
      medleyVariants = sched.medleyShield.map(ms => ms.generateVariants(ms.eligibleVariants)),
      medleyMinutes = sched.medleyShield.map(_.arenaMedleyMinutes),
      position = sched.position,
      mode = Mode.Rated,
      conditions = sched.conditions,
      schedule = Some(sched),
      startsAt = sched.at plusSeconds ThreadLocalRandom.nextInt(60),
      description = sched.medleyShield.map(_.arenaDescriptionFull),
      trophy1st = sched.medleyShield.map(_.key),
      trophyExpiryDays = if (sched.medleyShield.isDefined) 7.some else none,
      //we've scheduled this tour so make bots allowed for any of our tours
      botsAllowed = true
    )

  def tournamentUrl(tourId: String): String = s"https://playstrategy.org/tournament/$tourId"

  def makeId = ThreadLocalRandom nextString 8

  case class PastAndNext(past: List[Tournament], next: List[Tournament])

  sealed abstract class JoinResult(val error: Option[String]) {
    def ok = error.isEmpty
  }
  object JoinResult {
    case object Ok            extends JoinResult(none)
    case object WrongPassword extends JoinResult("Wrong password".some)
    case object Paused        extends JoinResult("Your pause is not over yet".some)
    case object Verdicts      extends JoinResult("Tournament restrictions".some)
    case object MissingTeam   extends JoinResult("Missing team".some)
    case object Nope          extends JoinResult("Couldn't join for some reason?".some)
  }
}
