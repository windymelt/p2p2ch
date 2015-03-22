package controllers

import models.ThreadHeader
import controllers.localdb.LocalDatabase
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz._
import Scalaz._
import Utility._BR_

object Subject {
  def generateSubject(chord2ch: ⇒ Chord2ch): String = {
    // DBからキャッシュを読んでDHTから取り出して文字データを再構成してThread型に変換してsubject形式にする
    import scala.concurrent.ExecutionContext.Implicits.global
    import play.api.libs.concurrent.Akka

    play.Logger.debug("generating subject.txt...")

    val threads = LocalDatabase.default.getThreads

    play.Logger.debug(s"Thread addresses in DB: ${threads.size}")

    val threadvalsF: (List[Array[Byte]]) ⇒ Future[List[Option[Stream[Byte]]]] = thr ⇒ Akka.future {
      Await.result(Future.sequence(thr.map {
        key ⇒ chord2ch.get(key.toSeq)
      }), 50 second)
    }

    val tokenize: (Stream[Byte]) ⇒ ThreadHeader = thr ⇒ (new String(thr.toArray)).split("""<>""") |> ((s: Array[String]) ⇒ ThreadHeader(s(0), s(1).toLong, s(2), s(3), s(4)))

    val future_list_opt_mapper: (Stream[Byte] ⇒ ThreadHeader) ⇒ Future[List[Option[Stream[Byte]]]] ⇒ List[ThreadHeader] =
      f ⇒ flo ⇒
        Await.result(flo map (lis ⇒ lis.map(opt ⇒ opt map f).filterNot(_.isEmpty).map(_.get)), 100 seconds)

    val genBody: (List[ThreadHeader]) ⇒ String = t ⇒ views.html.threadList(t).body

    val body: String = threads |> (threadvalsF >>> (tokenize |> future_list_opt_mapper) >>> genBody)

    play.Logger.debug(s"subject.txt has been generated.(${threads.size})")

    "0.dat<>P2P2chの情報 (1)" + _BR_ + body
  }
}
