package controllers.threadwriting

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ Request }

class ThreadWritingFormExtractor(implicit request: Request[Map[String, Seq[String]]]) {
  def extract: WriteRequestR = {
    val WriteRequestForm = Form(mapping(
      "bbs" → text,
      "key" → longNumber,
      "time" → text,
      "submit" → text,
      "FROM" → text,
      "mail" → text,
      "MESSAGE" → text
    )(WriteRequestR.apply)(WriteRequestR.unapply))
    WriteRequestForm.bindFromRequest().get
  }
}
