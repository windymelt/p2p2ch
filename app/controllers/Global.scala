import play.api.{ Logger, Application, GlobalSettings }

object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    Logger.debug("application started")
  }

  override def onStop(app: Application) = {
    Logger.debug("shutting down application..")
    controllers.Application.stopping()
  }
}
