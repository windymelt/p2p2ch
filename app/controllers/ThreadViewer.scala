package controllers

import controllers.digest.Digest
import models.{ Responses, Thread, Response, ThreadHeader }
import controllers.dht.DHT
import controllers.Utility._
import play.api.Logger
import scala.concurrent.duration._
import scala.concurrent.Await
import localdb.LocalDatabase
import scalaz._
import Scalaz._

class ThreadViewer {
  def loadThread(datNumber: Long): Option[models.Thread] = {
    import scala.util.control.Exception._
    import org.apache.commons.codec.binary.Base64
    Logger.info(s"loading thread: $datNumber")

    val threadKeyOpt = LocalDatabase.default.fetchThreadKeyFromDatNumber(datNumber)
    if (threadKeyOpt.isEmpty) { return None }
    val threadData = Await.result(DHT.default.get(threadKeyOpt.get.toSeq), 10 seconds).toOption

    Logger.trace("thread key: " + Digest.base64(threadKeyOpt.get))
    val responseKeys = threadKeyOpt map LocalDatabase.default.fetchResponseKeysFromThreadKey get;
    Logger.trace("Response keys: ")
    responseKeys.map(arr ⇒ Digest.base64(arr)).foreach(str ⇒ Logger.trace(str))

    threadData match {
      case Some(threadData: Stream[Byte]) ⇒
        Logger.trace("processing stream: " + new String(threadData.toArray))
        val threadDataSplited = new String(threadData.toArray[Byte]).split("<>")
        val threadC = ThreadHeader(threadDataSplited(0), threadDataSplited(1).toLong, threadDataSplited(2), threadDataSplited(3), threadDataSplited(4))
        val responses = responseKeys.map {
          case response: Array[Byte] ⇒
            allCatch opt {
              Await.result(DHT.default.get(response.toSeq), 10 seconds).toOption
            }
          case otherwise ⇒ None
          /*}.filterNot {
            _.isEmpty*/
        }.flatten.collect {
          case Some(v: Stream[Byte]) ⇒
            val splited = new String(v.toArray).split("<>")
            Response.toLocalized(Response(Base64.decodeBase64(splited(0).getBytes), splited(1), splited(2), splited(3), splited(4).toLong))
        }
        Thread(threadC, Responses(responses)).some

      case None ⇒ None
    }
  }
  def convertThread2HTML(thread: Thread): String = {
    views.html.thread(List(
      (thread.header.from, thread.header.mail, Otimestamp2str(Some(thread.header.since)), thread.header.body, thread.header.title)
    ) ++
      thread.responses.responses.map {
        ar ⇒ (ar.name, ar.mail, Otimestamp2str(Some(ar.time)), ar.body, "")
      }.toList).body
  }
}
