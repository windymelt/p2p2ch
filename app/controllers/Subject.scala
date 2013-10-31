package controllers

import play.api.db._
import play.api.Play.current
import anorm._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz._
import Scalaz._

object Subject {

  def generateSubject(chord2ch: => Chord2ch): String = {
    // DBからキャッシュを読んでDHTから取り出して文字データを再構成してThread型に変換してsubject形式にする
    import scala.concurrent.ExecutionContext.Implicits.global
    import play.api.libs.concurrent.Akka

    play.Logger.debug("generating subject.txt...")

    val threads = DB.withConnection {
      implicit c =>
        SQL("SELECT THREAD FROM THREAD_CACHE")().map {
          case Row(thread: Array[Byte]) => thread
        }.toList
    }

    val threadvalsF: (List[Array[Byte]]) => Future[List[Option[Stream[Byte]]]] = thr => Akka.future {
      Await.result(Future.sequence(thr.map {
        key => chord2ch.get(key.toSeq)
      }), 50 second)
    }

    val tokenize: (Stream[Byte]) => Thread = thr => (new String(thr.toArray)).split( """<>""") |> ((s: Array[String]) => Thread(s(0), s(1).toLong, s(2), s(3), s(4)))

    val future_list_opt_mapper: (Stream[Byte] => Thread) => Future[List[Option[Stream[Byte]]]] => List[Thread] =
      f => flo =>
        Await.result(flo map (lis => lis.map(opt => opt ∘ f).filterNot(_.isEmpty).sequence[Option[List[Thread]], Thread].copoint), 100 seconds)

    val genBody: (List[Thread]) => String = t => views.html.subject(t).body

    val body: String = threads |> (threadvalsF >>> (tokenize |> future_list_opt_mapper) >>> genBody)

    play.Logger.debug(s"subject.txt has been generated.(${threads.size})")

    "0.dat<>P2P2chの情報 (1)\n" + body
  }
}
