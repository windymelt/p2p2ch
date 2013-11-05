package controllers

/*
object Threads {
  import play.api._
  import play.api.mvc._
  import play.api.data._
  import play.api.data.Forms._
  import play.api.db._
  import play.api.Play.current
  import anorm._
  import momijikawa.p2pscalaproto._
  import akka.actor.{ActorContext, ActorRef}
  import java.security.MessageDigest
  import scala.concurrent.Await
  import scala.concurrent.duration._
  import Application.chord2ch
  import controllers.Utility._
  import scalaz._
  import Scalaz._

  def showOne(dat: String): \/[String, Array[Byte]] = { // must error opt guard
    import controllers.Response
    import scala.util.control.Exception._
    import org.apache.commons.codec.binary.Base64

    type ResponseTriple = (Array[Byte], Array[Byte], Long)
    type StringE[T] = \/[String, T]

    Logger.info(s"loading thread: $dat")
    val toDatLong: String => \/[String, Long] =
      str => failAsValue(classOf[Exception])("cannot convert into long".left)(str.substring(0, str.lastIndexOf(".")).toLong.right)

    val getFromDatabase = (dLong: Long) => DB.withConnection {
      implicit c =>
        SQL(
          """SELECT T.THREAD, R.RESPONSE, R.MODIFIED
            | FROM THREAD_CACHE AS T, RESPONSE_CACHE AS R
            | WHERE T.MODIFIED = {modified} AND T.THREAD=R.THREAD
            | ORDER BY R.MODIFIED"""
        ).on("modified" -> dLong)().map {
          case Row(thread: Array[Byte], response: Array[Byte], modified: Long) => (thread, response, modified).right
          case _ => "data not found on the local database".left
        }.toList.sequence[StringE[ResponseTriple], ResponseTriple]
    }

    val takingThreadAddress: (List[(Array[Byte], Array[Byte], Long)]) => Array[Byte] = _(0)._1

    val loadThreadDataFromDHT = (threadKey: Array[Byte]) => Await.result(chord2ch.get(threadKey.toSeq), 10 seconds)

    val splited2Thread = (spl: List[String]) => Thread(spl(0), spl(1).toLong, spl(2), spl(3), spl(4))
    val splited2Response = (spl: List[List[String]], thrID: Array[Byte]) => spl map (s => Response(thrID, s(0), s(1), s(2), s(3).toLong))

    val loadResponseDataFromDHT = (responseKeys: List[ResponseTriple]) =>
      responseKeys map {case ((_, r: Array[Byte], _)) => allCatch opt (Await.result(chord2ch.get(r.toSeq), 10 seconds)) flatten}

    val responseData2pllitedResponse = (resData: Stream[Byte]) => new String(resData.toArray[Byte]).split("""<>""").toSeq.toList

    val thread_res_toBody: ((\/[String, Thread], \/[String, List[Response]])) => \/[String, Array[Byte]] =
      {
      case (\/-(t), \/-(r)) => views.html.threadC(t, r).body.getBytes("shift_jis").right
      case (-\/(t), _) => t.left
      case (_, -\/(r)) => r.left
    }

    val getThreadFromDHT: List[ResponseTriple] => \/[String, Thread] = arr =>
      {(arr(0)._1 |> loadThreadDataFromDHT) >>= (stream_spliting >>> splited2Thread >>> (_.some)) }.toSuccess("failed loading thread from dht").disjunction

    val getResponsesFromDHT: (List[ResponseTriple]) => \/[String, List[Response]] =
      (
        (loadResponseDataFromDHT >>> (_ map(_ map (responseData2pllitedResponse))) >>> filterNotEmpty)
          &&&
          takingThreadAddress
        ) >>> splited2Response.tupled >>> {
        case lis if lis.isEmpty => "No responses from DHT." left
        case lis => lis right
      }

    (dat |> toDatLong).flatMap(getFromDatabase).flatMap(_|> (getThreadFromDHT &&& getResponsesFromDHT) >>> thread_res_toBody)
  }
}
*/