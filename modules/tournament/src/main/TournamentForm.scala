package lila.tournament

import cats.implicits._
import strategygames.format.FEN
import strategygames.chess.{ StartingPosition }
import strategygames.{ Clock, GameFamily, GameLogic, Mode }
import strategygames.variant.Variant
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation
import play.api.data.validation.Constraint

import lila.common.Form._
import lila.hub.LeaderTeam
import lila.hub.LightTeam._
import lila.user.User

final class TournamentForm {

  import TournamentForm._

  def create(user: User, leaderTeams: List[LeaderTeam], teamBattleId: Option[TeamID] = None) =
    form(user, leaderTeams) fill TournamentSetup(
      name = teamBattleId.isEmpty option user.titleUsername,
      clockTime = clockTimeDefault,
      clockIncrement = clockIncrementDefault,
      minutes = minuteDefault,
      waitMinutes = waitMinuteDefault.some,
      startDate = none,
      variant = s"${GameFamily.Chess().id}_${Variant.default(GameLogic.Chess()).id}".some,
      medley = false.some,
      medleyMinutes = medleyMinutesDefault.some,
      medleyDefaults = MedleyDefaults(
        onePerGameFamily = false.some,
        exoticChessVariants = false.some,
        draughts64Variants = false.some
      ),
      medleyGameFamilies = MedleyGameFamilies(
        chess = true.some,
        draughts = true.some,
        shogi = true.some,
        xiangqi = true.some,
        loa = true.some,
        flipello = true.some,
        mancala = true.some
      ),
      position = None,
      password = None,
      mode = none,
      rated = true.some,
      conditions = Condition.DataForm.AllSetup.default,
      teamBattleByTeam = teamBattleId,
      berserkable = true.some,
      streakable = true.some,
      description = none,
      hasChat = true.some
    )

  def edit(user: User, leaderTeams: List[LeaderTeam], tour: Tournament) =
    form(user, leaderTeams) fill TournamentSetup(
      name = tour.name.some,
      clockTime = tour.clock.limitInMinutes,
      clockIncrement = tour.clock.incrementSeconds,
      minutes = tour.minutes,
      waitMinutes = none,
      startDate = tour.startsAt.some,
      variant = s"${tour.variant.gameFamily.id}_${tour.variant.id}".some,
      medley = tour.isMedley.some,
      medleyMinutes = tour.medleyMinutes,
      medleyDefaults = MedleyDefaults(
        onePerGameFamily = onePerGameFamilyInMedley(tour.medleyVariants).some,
        exoticChessVariants = exoticChessVariants(tour.medleyVariants).some,
        draughts64Variants = draughts64Variants(tour.medleyVariants).some
      ),
      medleyGameFamilies = MedleyGameFamilies(
        chess = gameFamilyInMedley(tour.medleyVariants, GameFamily.Chess()).some,
        draughts = gameFamilyInMedley(tour.medleyVariants, GameFamily.Draughts()).some,
        shogi = gameFamilyInMedley(tour.medleyVariants, GameFamily.Shogi()).some,
        xiangqi = gameFamilyInMedley(tour.medleyVariants, GameFamily.Xiangqi()).some,
        loa = gameFamilyInMedley(tour.medleyVariants, GameFamily.LinesOfAction()).some,
        flipello = gameFamilyInMedley(tour.medleyVariants, GameFamily.Flipello()).some,
        mancala = gameFamilyInMedley(tour.medleyVariants, GameFamily.Mancala()).some
      ),
      position = tour.position,
      mode = none,
      rated = tour.mode.rated.some,
      password = tour.password,
      conditions = Condition.DataForm.AllSetup(tour.conditions),
      teamBattleByTeam = none,
      berserkable = tour.berserkable.some,
      streakable = tour.streakable.some,
      description = tour.description,
      hasChat = tour.hasChat.some
    )

  private val blockList = List("playstrategy", "lichess")

  private def nameType(user: User) = eventName(2, 30).verifying(
    Constraint[String] { (t: String) =>
      if (blockList.exists(t.toLowerCase.contains) && !user.isVerified && !user.isAdmin)
        validation.Invalid(validation.ValidationError("Must not contain \"playstrategy\""))
      else validation.Valid
    }
  )

  private def medleyVariantsList(medleyVariants: Option[List[Variant]]) =
    medleyVariants.getOrElse(List[Variant]())

  private def gameFamilyInMedley(medleyVariants: Option[List[Variant]], gf: GameFamily) =
    medleyVariantsList(medleyVariants).map(v => v.gameFamily).contains(gf)

  private def onePerGameFamilyInMedley(medleyVariants: Option[List[Variant]]) = {
    val mvList       = medleyVariantsList(medleyVariants)
    val gameFamilies = mvList.map(_.gameFamily).distinct
    mvList.map(_.gameFamily).take(gameFamilies.size) == gameFamilies && gameFamilies.size > 1
  }

  private def exoticChessVariants(medleyVariants: Option[List[Variant]]) =
    medleyVariantsList(medleyVariants).filterNot(_.exoticChessVariant).isEmpty

  private def draughts64Variants(medleyVariants: Option[List[Variant]]) =
    medleyVariantsList(medleyVariants).filterNot(_.draughts64Variant).isEmpty

  private def form(user: User, leaderTeams: List[LeaderTeam]) =
    Form(
      mapping(
        "name"           -> optional(nameType(user)),
        "clockTime"      -> numberInDouble(clockTimeChoices),
        "clockIncrement" -> numberIn(clockIncrementChoices),
        "minutes" -> {
          if (lila.security.Granter(_.ManageTournament)(user)) number
          else numberIn(minuteChoices)
        },
        "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
        "startDate"   -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
        "variant" -> optional(
          nonEmptyText.verifying(v =>
            Variant(GameFamily(v.split("_")(0).toInt).gameLogic, v.split("_")(1).toInt).isDefined
          )
        ),
        "medley"        -> optional(boolean),
        "medleyMinutes" -> optional(numberIn(medleyMinutes)),
        "medleyDefaults" -> mapping(
          "onePerGameFamily"    -> optional(boolean),
          "exoticChessVariants" -> optional(boolean),
          "draughts64Variants"  -> optional(boolean)
        )(MedleyDefaults.apply)(MedleyDefaults.unapply),
        "medleyGameFamilies" -> mapping(
          "chess"    -> optional(boolean),
          "draughts" -> optional(boolean),
          "shogi"    -> optional(boolean),
          "xiangqi"  -> optional(boolean),
          "loa"      -> optional(boolean),
          "flipello" -> optional(boolean),
          "mancala"  -> optional(boolean)
        )(MedleyGameFamilies.apply)(MedleyGameFamilies.unapply),
        "position"         -> optional(lila.common.Form.fen.playableStrict),
        "mode"             -> optional(number.verifying(Mode.all.map(_.id) contains _)), // deprecated, use rated
        "rated"            -> optional(boolean),
        "password"         -> optional(cleanNonEmptyText),
        "conditions"       -> Condition.DataForm.all(leaderTeams),
        "teamBattleByTeam" -> optional(nonEmptyText.verifying(id => leaderTeams.exists(_.id == id))),
        "berserkable"      -> optional(boolean),
        "streakable"       -> optional(boolean),
        "description"      -> optional(cleanNonEmptyText),
        "hasChat"          -> optional(boolean)
      )(TournamentSetup.apply)(TournamentSetup.unapply)
        .verifying("Invalid clock", _.validClock)
        .verifying("15s and 0+1 variant games cannot be rated", _.validRatedVariant)
        .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
        .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration)
    )
}

object TournamentForm {

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ {
    (2 to 7 by 1) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  }.map(_.toDouble)
  val clockTimeDefault = 2d
  private def formatLimit(l: Double) =
    Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " minute" else " minutes"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)

  val clockIncrements       = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val minutes       = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30) ++ (420 to 600 by 60) :+ 720
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")

  val medleyMinutes        = (5 to 30 by 5) ++ (40 to 40) ++ (45 to 60 by 15)
  val medleyMinuteChoices  = options(medleyMinutes, "%d minute{s}")
  val medleyMinutesDefault = 10

  val waitMinutes       = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  val validVariants = Variant.all.filter(!_.fromPositionVariant)

  def guessVariant(from: String): Option[Variant] =
    validVariants.find { v =>
      v.key == from || from.toIntOption.exists(v.id ==)
    }

  val joinForm =
    Form(
      mapping(
        "team"     -> optional(nonEmptyText),
        "password" -> optional(nonEmptyText)
      )(TournamentJoin.apply)(TournamentJoin.unapply)
    )

  case class TournamentJoin(team: Option[String], password: Option[String])
}

private[tournament] case class TournamentSetup(
    name: Option[String],
    clockTime: Double,
    clockIncrement: Int,
    minutes: Int,
    waitMinutes: Option[Int],
    startDate: Option[DateTime],
    variant: Option[String],
    medley: Option[Boolean],
    medleyMinutes: Option[Int],
    medleyDefaults: MedleyDefaults,
    medleyGameFamilies: MedleyGameFamilies,
    position: Option[FEN],
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    conditions: Condition.DataForm.AllSetup,
    teamBattleByTeam: Option[String],
    berserkable: Option[Boolean],
    streakable: Option[Boolean],
    description: Option[String],
    hasChat: Option[Boolean]
) {

  def validClock = (clockTime + clockIncrement) > 0

  def realMode =
    if (realPosition.isDefined) Mode.Casual
    else Mode(rated.orElse(mode.map(Mode.Rated.id ===)) | true)

  def gameLogic = variant match {
    case Some(v) => GameFamily(v.split("_")(0).toInt).gameLogic
    case None    => GameLogic.Chess()
  }

  def realVariant = variant flatMap { v =>
    Variant.apply(gameLogic, v.split("_")(1).toInt)
  } getOrElse Variant.default(gameLogic)

  def realPosition = position ifTrue realVariant.standardVariant

  def clockConfig = Clock.Config((clockTime * 60).toInt, clockIncrement)

  def validRatedVariant =
    realMode == Mode.Casual ||
      lila.game.Game.allowRated(realVariant, clockConfig.some)

  def sufficientDuration = estimateNumberOfGamesOneCanPlay >= 3
  def excessiveDuration  = estimateNumberOfGamesOneCanPlay <= 150

  def isPrivate = password.isDefined || conditions.teamMember.isDefined

  // update all fields and use default values for missing fields
  // meant for HTML form updates
  def updateAll(old: Tournament): Tournament = {
    val newVariant = if (old.isCreated && variant.isDefined) realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if (old.isCreated) clockConfig else old.clock,
        minutes = minutes,
        mode = realMode,
        variant = newVariant,
        medleyVariants =
          if (
            old.medleyGameFamilies != medleyGameFamilies.gfList
              .sortWith(_.name < _.name)
              .some || old.medleyMinutes != medleyMinutes || old.minutes != minutes
          ) medleyVariants
          else old.medleyVariants,
        medleyMinutes = medleyMinutes,
        startsAt = startDate | old.startsAt,
        password = password,
        position = newVariant.standardVariant ?? {
          if (old.isCreated || old.position.isDefined) realPosition
          else old.position
        },
        noBerserk = !(~berserkable),
        noStreak = !(~streakable),
        teamBattle = old.teamBattle,
        description = description,
        hasChat = hasChat | true
      )
  }

  // update only fields that are specified
  // meant for API updates
  def updatePresent(old: Tournament): Tournament = {
    val newVariant = if (old.isCreated) realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if (old.isCreated) clockConfig else old.clock,
        minutes = minutes,
        mode = if (rated.isDefined) realMode else old.mode,
        variant = newVariant,
        startsAt = startDate | old.startsAt,
        password = password.fold(old.password)(_.some.filter(_.nonEmpty)),
        position = newVariant.standardVariant ?? {
          if (position.isDefined && (old.isCreated || old.position.isDefined)) realPosition
          else old.position
        },
        noBerserk = berserkable.fold(old.noBerserk)(!_),
        noStreak = streakable.fold(old.noStreak)(!_),
        teamBattle = old.teamBattle,
        description = description.fold(old.description)(_.some.filter(_.nonEmpty)),
        hasChat = hasChat | old.hasChat
      )
  }

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement) * 2 * 0.8
  } + 15

  def isMedley = (medley | false) && medleyGameFamilies.gfList.nonEmpty

  def maxMedleyRounds = medleyMinutes.map(mm => Math.ceil(minutes.toDouble / mm).toInt)

  //shuffle all variants from the selected game families
  private lazy val generateNoDefaultsMedleyVariants: List[Variant] =
    scala.util.Random
      .shuffle(
        Variant.all.filter(v => medleyGameFamilies.gfList.contains(v.gameFamily) && !v.fromPositionVariant)
      )

  private def generateMedleyVariants: List[Variant] =
    if (medleyDefaults.onePerGameFamily.getOrElse(false)) {
      //take a shuffled list of all variants and pull the first for each game family to the front
      val onePerGameFamilyVariantList = scala.util.Random.shuffle(
        medleyGameFamilies.gfList.map(gf => generateNoDefaultsMedleyVariants.filter(_.gameFamily == gf).head)
      )
      onePerGameFamilyVariantList ::: generateNoDefaultsMedleyVariants.filterNot(
        onePerGameFamilyVariantList.contains(_)
      )
    } else if (medleyDefaults.exoticChessVariants.getOrElse(false))
      scala.util.Random.shuffle(Variant.all.filter(_.exoticChessVariant))
    else if (medleyDefaults.draughts64Variants.getOrElse(false))
      scala.util.Random.shuffle(Variant.all.filter(_.draughts64Variant))
    else generateNoDefaultsMedleyVariants

  def medleyVariants: Option[List[Variant]] =
    if (isMedley) {
      val medleyList     = generateMedleyVariants
      var fullMedleyList = medleyList
      while (fullMedleyList.size < maxMedleyRounds.getOrElse(0))
        fullMedleyList = fullMedleyList ::: medleyList
      fullMedleyList.some
    } else None
}

case class MedleyDefaults(
    onePerGameFamily: Option[Boolean],
    exoticChessVariants: Option[Boolean],
    draughts64Variants: Option[Boolean]
)

case class MedleyGameFamilies(
    chess: Option[Boolean],
    draughts: Option[Boolean],
    shogi: Option[Boolean],
    xiangqi: Option[Boolean],
    loa: Option[Boolean],
    flipello: Option[Boolean],
    mancala: Option[Boolean]
) {

  lazy val gfList: List[GameFamily] = GameFamily.all
    .filterNot(gf => if (!chess.getOrElse(false)) gf == GameFamily.Chess() else false)
    .filterNot(gf => if (!draughts.getOrElse(false)) gf == GameFamily.Draughts() else false)
    .filterNot(gf => if (!shogi.getOrElse(false)) gf == GameFamily.Shogi() else false)
    .filterNot(gf => if (!xiangqi.getOrElse(false)) gf == GameFamily.Xiangqi() else false)
    .filterNot(gf => if (!loa.getOrElse(false)) gf == GameFamily.LinesOfAction() else false)
    .filterNot(gf => if (!flipello.getOrElse(false)) gf == GameFamily.Flipello() else false)
    .filterNot(gf => if (!mancala.getOrElse(false)) gf == GameFamily.Mancala() else false)

}
