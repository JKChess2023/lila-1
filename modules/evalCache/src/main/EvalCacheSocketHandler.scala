package lila.evalCache

import strategygames.variant.Variant
import strategygames.format.FEN
import strategygames.GameLib

import play.api.libs.json._

import lila.socket._
import lila.user.User

final private class EvalCacheSocketHandler(
    api: EvalCacheApi,
    truster: EvalCacheTruster,
    upgrade: EvalCacheUpgrade
)(implicit ec: scala.concurrent.ExecutionContext) {

  def evalGet(
      sri: Socket.Sri,
      d: JsObject,
      push: JsObject => Unit
  ): Unit =
    for {
      fen <- d str "fen" map{fen => FEN.apply(GameLib(d int "lib" match {
        case Some(lib) => lib
        case None      => sys.error("lib must be provided for eval cache")
      }), fen)}
      variant = Variant.orDefault(GameLib(d int "lib" match {
        case Some(lib) => lib
        case None      => sys.error("lib must be provided for eval cache")
      }), ~d.str("variant"))
      multiPv = (d int "mpv") | 1
      path <- d str "path"
    } {
      def pushData(data: JsObject) = push(Socket.makeMessage("evalHit", data))
      api.getEvalJson(variant, fen, multiPv) foreach {
        _ foreach { json =>
          pushData(json + ("path" -> JsString(path)))
        }
      }
      if (d.value contains "up") upgrade.register(sri, variant, fen, multiPv, path)(pushData)
    }

  def untrustedEvalPut(sri: Socket.Sri, userId: User.ID, data: JsObject): Unit =
    truster cachedTrusted userId foreach {
      _ foreach { tu =>
        JsonHandlers.readPutData(tu, data) foreach {
          api.put(tu, _, sri)
        }
      }
    }
}
