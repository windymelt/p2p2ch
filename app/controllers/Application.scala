/**
 * This file is written in UTF-8.
 */
package controllers

import controllers.threadwriting.ThreadWriter
import controllers.threadbuilding.{ ThreadBuilder, ThreadBuildingFormExtractor }
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Promise
import play.api.libs.Comet
import momijikawa.p2pscalaproto._
import scalaz._
import Scalaz._
import scala.concurrent.duration._

object Application extends Controller {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  implicit val myCustomCharset = Codec.javaSupported("Shift_JIS")
  val chord2ch = new Chord2ch
  chord2ch.init(TnodeID.newNodeId)

  def shutdownHook() = {
    chord2ch.close()
  }

  def showIndex = Action { Ok(views.html.index("Your new application is ready.")) }
  def showBBSIndex = Action { Ok(views.html.bbsindex("test")).as(HTML) }
  def showBBSMenu = Action { Ok(views.html.bbsmenu()).as(HTML) }
  def showBBSTable = Action { Ok(views.html.bbstable()).as(HTML) }
  def showSubject = Action { Ok(Subject.generateSubject(chord2ch).getBytes("shift_jis")).as("text/plain") }
  def showInformation = Action { Ok(Information.getInformation.getBytes("shift_jis")).as("text/plain") }
  def showStatusImage = Action { Ok(StatusGraph.getStatusImage(chord2ch.getStatus)).as("image/png").withHeaders("Cache-Control" -> "no-cache") }
  def showStatusImageWithRefresh(interval: Int = 30) = Action { Ok(views.html.statusImageWithRefresh(interval)).as(HTML) }
  def showStatusGraphRealtime = Action { Ok(views.html.statusImageRealtime()).as(HTML).withHeaders("Cache-Control" -> "no-cache") }

  def showThread(datFileName: String) = Action {
    val numberPartOfDatFileName = datFileName.substring(0, datFileName.lastIndexOf("."))
    val datNumberOpt = Utility.string2LongOpt(numberPartOfDatFileName)
    val viewer = new ThreadViewer()
    val threadOpt = datNumberOpt >>= viewer.loadThread
    threadOpt match {
      case Some(thread) ⇒
        Ok(viewer.convertThread2HTML(thread).getBytes("shift_jis"))
          .as("text/plain")
          .withHeaders("Cache-Control" -> "no-cache")
      case None ⇒ Ok("failed to load thread")
    }
  }

  private def strMalSJIS2strU(str: String) = {
    new String(str.getBytes("Shift-JIS"), "utf-8")
  }

  case class WriteRequestR(bbs: String, key: Long, time: String, submit: String, FROM: String, mail: String, MESSAGE: String)

  def writeThread = Action {
    implicit request ⇒
      Logger.info(s"request encoding is: ${request.charset}")

      val subjectForm = Form("subject" -> text)
      subjectForm.bindFromRequest().value match {
        case Some(_) ⇒
          val params = new ThreadBuildingFormExtractor().extract
          val buildResult = new ThreadBuilder().buildThread(params.subject, params.FROM, params.mail, params.MESSAGE)
          buildResult match {
            case \/-(_)     ⇒ Ok(views.html.threadPostSuccessful()).as(HTML)
            case -\/(error) ⇒ Ok(views.html.threadBuildFailed(error.message)).as(HTML)
          }

        case None ⇒
          val WriteRequestForm = Form(mapping(
            "bbs" -> text,
            "key" -> longNumber,
            "time" -> text,
            "submit" -> text,
            "FROM" -> text,
            "mail" -> text,
            "MESSAGE" -> text)(WriteRequestR.apply)(WriteRequestR.unapply))
          val params = WriteRequestForm.bindFromRequest().get
          params.key match {
            case 0 ⇒
              Information.configurate(params.MESSAGE)
              Ok(views.html.threadPostSuccessful()).as(HTML)
            case threadDatNumber ⇒
              val writeResult = new ThreadWriter().writeThread(threadDatNumber, params.FROM, params.mail, params.MESSAGE)
              writeResult match {
                case \/-(_)     ⇒ Ok(views.html.threadPostSuccessful()).as(HTML)
                case -\/(error) ⇒ Ok(views.html.threadWriteFailed(error.message)).as(HTML)
              }
          }
      }
  }

  def showStatusGraphComet = Action {
    request ⇒
      val host = request.headers("Host")
      Ok.chunked(notifier &> Comet(callback = "parent.changed"))
  }

  lazy val notifier: Enumerator[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Enumerator.generateM {
      Promise.timeout(Some(chord2ch.stateAgt().toString), 100 milliseconds)
    }
  }
}
