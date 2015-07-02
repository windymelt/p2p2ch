import play.api.{ Logger, Application, GlobalSettings }
import play.api.mvc.RequestHeader

object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    Logger.debug("application started")
  }

  override def onStop(app: Application) = {
    Logger.debug("shutting down application..")
    controllers.Application.shutdownHook()
  }

  override def onRouteRequest(requestHeader: RequestHeader) = {
    // decode body as Shift_JIS and re-encode body as UTF-8
    identity("hoge")
    //val decodedBody = java.net.URLDecoder.decode(requestHeader.rawQueryString, "Shift_JIS")
    super.onRouteRequest(requestHeader)
  }
}
