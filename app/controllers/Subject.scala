package controllers

import controllers.Utility._BR_
import controllers.dht.DHT
import controllers.localdb.LocalDatabase
import models.ListOfThreadHeaders
import models.ThreadHeader
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.matching.Regex

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
      case None ⇒ Nil
    }
    val headersObject = new ListOfThreadHeaders(listOfheaders)
    val subjectString = headersObject.generateString
    play.Logger.debug(s"subject.txt has been generated.(${threadKeys.size})")

    subjectString
  }

  case class Thread(hash: String, title: String, response: String)

  def generateThreadList: List[Thread] = {

    val subject: String = "0.dat<>P2P2chの情報 (1)" + _BR_ + Subject.generateSubject
    play.Logger.debug(s"subject.txt has been generated like following.(${subject})")

    val RegexThread: Regex = """([0-9]*).dat<>(.+)\(([0-9]*)\)""".r
    var threadList: List[Thread] = List()

    subject.split("<br>").foreach(
      e => RegexThread.findAllIn(e).matchData.foreach { m =>

        val hash: String = m.group(1).replaceAll(".dat", "")
        val title: String = m.group(2)
        val response: String = m.group(3)
        threadList = threadList :+ Thread(hash, title, response)
      }
    )

    threadList
  }
}
