/**
 * This file is written in UTF-8.
 */
package controllers

import controllers.threadwriting.{ ThreadWriter, ThreadWritingFormExtractor }
import controllers.threadbuilding.{ ThreadBuilder, ThreadBuildingFormExtractor }
import controllers.dht.DHT
import controllers.dht.DHTChord2ch
import controllers.Utility._BR_
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Promise
import play.api.libs.Comet
import scala.concurrent.duration._
import scalaz._
import Scalaz._

object Application extends Controller {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  println(Utility.logo) // おあそび

  implicit val myCustomCharset = Codec.javaSupported("Shift_JIS")
  DHT.default.initialize("")

  def shutdownHook() = {
    DHT.default.close()
  }

  def showIndex = Action { Ok(views.html.bbstable(Subject.generateThreadList)).as(HTML) }
  def showBBSTable = Action { Ok(views.html.bbstable(Subject.generateThreadList)).as(HTML) }
  def showBBSIndex = Action { Ok(views.html.bbsindex("test")).as(HTML) }
  def showBBSMenu = Action { Ok(views.html.bbsmenu()).as(HTML) }

  def showSubject = Action {
    Ok(("0.dat<>P2P2chの情報 (1)" + _BR_ + Subject.generateSubject).getBytes("shift_jis")).as("text/plain")
  }

  def showInformation = Action { Ok(Information.getInformation.getBytes("shift_jis")).as("text/plain") }
  def showStatusImage = Action { Ok(StatusGraph.getStatusImage(DHT.default.asInstanceOf[DHTChord2ch.type].getStatus.get)).as("image/png").withHeaders("Cache-Control" -> "no-cache") }
  def showStatusImageWithRefresh(interval: Int = 30) = Action { Ok(views.html.statusImageWithRefresh(interval)).as(HTML) }
  def showStatusGraphRealtime = Action { Ok(views.html.statusImageRealtime()).as(HTML).withHeaders("Cache-Control" -> "no-cache") }

  def showThread(datFileName: String) = Action {
    val numberPartOfDatFileName = datFileName.substring(0, datFileName.lastIndexOf("."))
    val datNumberOpt = Utility.string2LongOpt(numberPartOfDatFileName)
    val viewer = new ThreadViewer()
    val threadOpt = datNumberOpt >>= viewer.loadThread
    Logger.trace("thread load done.")
    threadOpt match {
      case Some(thread) ⇒
        Ok(viewer.convertThread2HTML(thread).getBytes("shift_jis"))
          .as("text/plain")
          .withHeaders("Cache-Control" → "no-cache")
      case None ⇒ Ok("failed to load thread")
    }
  }

  private def strMalSJIS2strU(str: String) = {
    new String(str.getBytes("Shift-JIS"), "utf-8")
  }

  def writeThread = Action(parse.tolerantText) { request ⇒
    implicit val newRequest = PercentEncoding.extractSJISRequest(request)
    Logger.info(s"request encoding is: ${request.charset}")

    val subjectForm = Form("subject" → text) // スレ建て時にはsubjectに値が入っている
    val isBuildingThread = subjectForm.bindFromRequest().value.isDefined
    if (isBuildingThread) {
      val params = new ThreadBuildingFormExtractor().extract
      val buildResult = new ThreadBuilder().buildThread(params.subject, params.FROM, params.mail, params.MESSAGE)
      buildResult match {
        case \/-(_)     ⇒ Ok(views.html.threadPostSuccessful()).as(HTML)
        case -\/(error) ⇒ Ok(views.html.threadBuildFailed(error.message)).as(HTML)
      }
    } else {
      val params = new ThreadWritingFormExtractor().extract
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
      Promise.timeout(Some(DHT.default.asInstanceOf[DHTChord2ch.type].getStatus.get.toString), 100 milliseconds)
    }
  }
}
