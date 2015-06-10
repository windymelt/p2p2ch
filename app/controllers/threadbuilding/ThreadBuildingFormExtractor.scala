package controllers.threadbuilding

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ AnyContent, Request }

class ThreadBuildingFormExtractor(implicit request: Request[AnyContent]) {
  def extract: WriteRequestT = {
    val BuildRequestForm = Form(mapping(
      "bbs" -> text,
      "time" -> text,
      "submit" -> text,
      "FROM" -> text,
      "mail" -> text,
      "MESSAGE" -> text,
      "subject" -> text
    )(WriteRequestT.apply)(WriteRequestT.unapply))
    BuildRequestForm.bindFromRequest().get
  }
}
