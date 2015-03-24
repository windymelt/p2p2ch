package controllers

import models.ThreadHeader
import models.ListOfThreadHeaders
import controllers.localdb.LocalDatabase
import controllers.dht.DHT
import scala.concurrent.Await
import scala.concurrent.duration._

object Subject {
  def generateSubject: String = {
    // DBからキャッシュを読んでDHTから取り出して文字データを再構成してThread型に変換してsubject形式にする
    import scala.concurrent.ExecutionContext.Implicits.global

    play.Logger.debug("generating subject.txt...")
    val threadKeys = LocalDatabase.default.getThreads
    play.Logger.debug(s"Thread addresses in DB: ${threadKeys.size}")
    val threadBytes: List[Option[Stream[Byte]]] = {
      threadKeys map { threadKey ⇒ Await.result(DHT.default.get(threadKey).map(_.toOption), 10 seconds) }
    }
    val listOfheaders = threadBytes flatMap {
      case Some(stream) ⇒ ThreadHeader.fromByteArray(stream.toArray).toList
      case None         ⇒ Nil
    }
    val headersObject = new ListOfThreadHeaders(listOfheaders)
    val subjectString = headersObject.generateString
    play.Logger.debug(s"subject.txt has been generated.(${threadKeys.size})")

    subjectString
  }
}
