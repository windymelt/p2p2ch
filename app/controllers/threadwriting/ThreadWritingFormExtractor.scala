package controllers.threadwriting

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ AnyContent, Request }

class ThreadWritingFormExtractor(implicit request: Request[AnyContent]) {
  def extract: WriteRequestR = {
    val WriteRequestForm = Form(mapping(
      "bbs" -> text,
      "key" -> longNumber,
      "time" -> text,
      "submit" -> text,
      "FROM" -> text,
      "mail" -> text,
      "MESSAGE" -> text)(WriteRequestR.apply)(WriteRequestR.unapply))
    WriteRequestForm.bindFromRequest().get
  }
}
