package lila.setup

import strategygames.GameLib
import strategygames.format.FEN

case class ValidFen(fen: FEN, situation: strategygames.Situation) {

  def color = situation.color
}

object ValidFen {

  def apply(strict: Boolean)(lib: GameLib, fen: FEN): Option[ValidFen] =
    for {
      parsed <- strategygames.format.Forsyth.<<<(lib, fen)
      if parsed.situation playable strict
      validated = strategygames.format.Forsyth.>>(lib, parsed)
    } yield ValidFen(validated, parsed.situation)
}
