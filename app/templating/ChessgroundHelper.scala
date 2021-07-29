package lila.app
package templating

//TODO: Muddled Pos here, chess specific stuff in here with ranks/files
import strategygames.chess.Pos
import strategygames.{Board, Color }
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

trait ChessgroundHelper {

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgHelper    = tag("cg-helper")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgHelper(cgContainer(cgBoard))

  def chessground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag =
    wrap {
      cgBoard {
        raw {
          if (ctx.pref.is3d) ""
          else {
            def top(p: Pos)  = orient.fold(7 - p.rank.index, p.rank.index) * 12.5
            def left(p: Pos) = orient.fold(p.file.index, 7 - p.file.index) * 12.5
            val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
              s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
            } mkString ""
            val pieces =
              if (ctx.pref.isBlindfold) ""
              else
                board.pieces.map { case (strategygames.Pos.Chess(pos), piece) =>
                  val klass = s"${piece.color.name} ${piece.role.name}"
                  s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
                  case (strategygames.Pos.Draughts(_), _) => sys.error("Draughts not supported yet")
                } mkString ""
            s"$highlights$pieces"
          }
        }
      }
    }

  def chessground(pov: Pov)(implicit ctx: Context): Frag =
    chessground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove.map(_.origDest) ?? {
        case (strategygames.Pos.Chess(orig), strategygames.Pos.Chess(dest)) => List(orig, dest)
        case (strategygames.Pos.Draughts(_), strategygames.Pos.Draughts(_)) => sys.error("Draughts not supported yet")
        case _ => sys.error("Unsupported games.")
      }
    )

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgHelper {
        cgContainer {
          content
        }
      }
    }

  lazy val chessgroundBoard = wrap(cgBoard)
}
