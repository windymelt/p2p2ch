/**
 * This file is written in UTF-8.
 */
package controllers

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

object Application extends Controller {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  import scala.concurrent.Future
  import scala.concurrent.Await
  import scala.concurrent.duration._

  implicit val myCustomCharset = Codec.javaSupported("Shift_JIS")
  val chord2ch = new Chord2ch
  chord2ch.init(TnodeID.newNodeId)

  val Otimestamp2str: Option[Long] => String =
    (Otime: Option[Long]) => Otime.flatMap(t => Some(new java.util.Date(t * 1000).toString)).getOrElse("???");

  //println("controller!!!!!!!!!!!!!!!!!!!!!")
  def stopping = {
    chord2ch.close()
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def bbsindex = Action {
    Ok(views.html.bbsindex("test")).as(HTML)
  }

  def bbsmenu = Action {
    Ok("<BR><BR><B>P2P</B><BR>\n" +
      "<A HREF=/bbs/>P2P2ch</A><br>").as(HTML)
  }

  def bbstable = Action {
    Ok("【<B>P2P</B>】<A HREF=/bbs/>P2P2ch</A>").as(HTML)
  }

  def subject = Action {

    import scala.concurrent.ExecutionContext.Implicits.global
    import play.api.libs.concurrent.Akka

    play.Logger.debug("generating subject.txt...")
    //    println("loading subject.txt ...")
    val threads = DB.withConnection {
      implicit c =>
        SQL("SELECT THREAD FROM THREAD_CACHE")().map {
          case Row(thread: Array[Byte]) => thread
        }.toList
    }
    Async {
      val threadvalsF = Akka.future {
        Await.result(Future.sequence(threads.map {
          key => chord2ch.get(key.toSeq)
        }), 50 second)
      }
      threadvalsF.map {
        threadvals =>
          val threadclasses = threadvals.map {
            v: Option[Stream[Byte]] =>
              v map {
                bs: Stream[Byte] =>
                  val df = java.text.DateFormat.getDateTimeInstance
                  val strlis = new String(bs.toArray) split ("<>")
                  Thread(strlis(0), strlis(1).toLong, strlis(2), strlis(3), strlis(4))
              }
          }
          val body = views.html.subject(threadclasses.toList.filterNot(_.isEmpty).map {
            _.get
          }).body
          play.Logger.debug(s"subject.txt has been generated.(${threads.size})")
          Ok(("0.dat<>P2P2chの情報 (1)\n12345678.dat<>test (1)\n" + body).getBytes("shift_jis")).as("text/plain")
      }
    }
  }

  def information = Action {
    Logger.info("information has selected.")
    val str = "</b>INFO<b><><>INFORMATION<>このページにコマンドを書き込むことで各種の設定が可能です。\"help\"でコマンド一覧が表示されます。<>P2P2chの情報"
    val log = DB.withConnection {
      implicit c =>
        SQL("SELECT MESSAGE, MODIFIED FROM SETTING_LOG ORDER BY ID")().map {
          row => (row[String]("MESSAGE"), row[Option[Long]]("MODIFIED"))
        }.toList
    }.map {
      (t: (String, Option[Long])) => s"</b>INFO<b><>${Otimestamp2str(t._2)}<>INFORMATION<>${t._1}<>"
    }.mkString("\n")
    val result = log match {
      case "" => str
      case s => str + "\n" + log
    }
    val convertedByteArray = result.getBytes("shift_jis")
    Ok(convertedByteArray).as("text/plain")
  }

  def config_information(from: String, mail: String, message: String) = {
    val d = System.currentTimeMillis() / 1000
    val result = main_configuration(message)
    DB.withConnection {
      implicit c =>
        SQL("INSERT INTO SETTING_LOG (MESSAGE, MODIFIED) VALUES ({message}, {now})").on("message" -> result, "now" -> d).executeUpdate
    }
    Ok(views.html.successfullyWritten()).as(HTML)
  }

  def main_configuration(message: String): String = {
    message.nonEmpty match {
      case true =>
        message.split('\n').toList match {
          case "help" :: Nil =>
            """
              |join -- ノードと接続する
              |Example:
              |join
              |nodeID
              |references
              |
              |reference -- ノードの接続キーを表示する
              |
              |upload (path) -- ファイルをアップロードする
              |
              |help -- このコマンドを表示する
            """.stripMargin.replace("\n", "<br>")
          case "reference" :: Nil => chord2ch.getReference.getOrElse("N/A") replace("\n", "<br>")
          case "join" :: nodeid :: reference :: Nil =>
            chord2ch.join(nodeid + "\n" + reference); "接続を試行します"
          case "status" :: Nil =>
            val cst: ChordState = chord2ch.getStatus
            s"""
              |Self: ${cst.selfID.map {
              _.getNodeID
            }.getOrElse("N/A")}
              |Succ:
              |${cst.succList.nodes.list.mkString("\n")}
              |
              |Finger:
              |${cst.fingerList.nodes.list.mkString("\n")}
              |
              |Pred: ${cst.pred.map {
              _.getNodeID
            }.getOrElse("N/A")}
              |Data: ${cst.dataholder.size}""".stripMargin.replace("\n", "<br>")
          //.toString.replace("\n", "<br>")
          case upload :: Nil => upload.split(" ").toList match {
            case "upload" :: path :: Nil => "まだ実装されてない"
            case _ => "そんなコマンド知らん"
          }
          case _ => "そんなコマンド知らん"
        }
      case false => "空白は困ります"
    }
  }

  def showThread(dat: String) = Action {
    import scala.util.control.Exception._
    import org.apache.commons.codec.binary.Base64
    Logger.info(s"loading thread: $dat")
    //Ok(dat).as(HTML)
    val datNo = dat.substring(0, dat.lastIndexOf(".")).toLong
    val threadv = DB.withConnection {
      implicit c =>
        SQL("SELECT THREAD FROM THREAD_CACHE WHERE MODIFIED = {modified}").on("modified" -> datNo)().map {
          case Row(thread: Array[Byte]) => Some(thread)
          case _ => None
        }.filterNot(_.isEmpty).toList
    }
    val threadKey = threadv(0).get
    val responsesKeys = DB.withConnection {
      implicit c =>
        SQL("SELECT RESPONSE, MODIFIED FROM RESPONSE_CACHE WHERE THREAD = {thread}  ORDER BY MODIFIED").on("thread" -> threadKey)().map {
          case Row(response: Array[Byte], modified: Long) => (response, modified)
        }.toList
    }
    val threadData = (allCatch opt {
      Await.result(chord2ch.get(threadKey.toSeq), 10 second)
    }).flatten
    threadData match {
      case Some(threadData: Stream[Byte]) =>
        val threadDataSplited = new String(threadData.toArray[Byte]).split("<>")
        val threadC = Thread(threadDataSplited(0), threadDataSplited(1).toLong, threadDataSplited(2), threadDataSplited(3), threadDataSplited(4))
        val responses = responsesKeys.map {
          case (response: Array[Byte], modified: Long) =>
            allCatch opt {
              Await.result(chord2ch.get(response.toSeq), 10 second)
            }
          case otherwise => None
        }.filterNot {
          _.isEmpty
        }.flatten.map {
          case Some(v: Stream[Byte]) =>
            val splited = new String(v.toArray).split("<>")
            Response(Base64.decodeBase64(splited(0).getBytes), splited(1), splited(2), splited(3), splited(4).toLong)
        }
        Ok(
          views.html.thread(List((threadC.from, threadC.mail, Otimestamp2str(Some(threadC.since)), threadC.body, threadC.title)) ++
            responses.map {
              ar => (ar.name, ar.mail, Otimestamp2str(Some(ar.time)), ar.body, "")
            }.toList).body.getBytes("shift_jis")).as("text/plain")

      case None => Ok("failed to load thread")
    }

  }

  private def strMalSJIS2strU(str: String) = {
    new String(str.getBytes("Shift-JIS"), "utf-8")
  }

  case class WriteRequestT(bbs: String, time: String, submit: String, FROM: String, mail: String, MESSAGE: String, subject: String)

  case class WriteRequestR(bbs: String, key: Long, time: String, submit: String, FROM: String, mail: String, MESSAGE: String)

  def writeThread = Action {
    implicit request =>
      val subjectForm = Form("subject" -> text)
      subjectForm.bindFromRequest().value match {
        case Some(_) =>
          val WriteRequestForm = Form(mapping(
            "bbs" -> text,
            "time" -> text,
            "submit" -> text,
            "FROM" -> text,
            "mail" -> text,
            "MESSAGE" -> text,
            "subject" -> text)(WriteRequestT.apply)(WriteRequestT.unapply))
          val params = WriteRequestForm.bindFromRequest().get
          //val subj = request.body.asFormUrlEncoded.get.apply("subject")
          buildThread(/*strMalSJIS2strU(*/ params /*params.subject*/) //)

        case None =>
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
            case 0 => config_information(params.FROM, params.mail, params.MESSAGE)
            case _ => writeThreadMain(params)
          }
      }
  }

  private def writeThreadMain(params: WriteRequestR) = {
    // すれたてるときにcurrenttimemilの衝突がおこるかも
    // put into chord
    //keyに対応するthreadを取得する
    Logger.info("writing response.")
    Logger.debug("checking whether there's thread key in local database.")
    val keys = DB.withConnection {
      implicit c =>
        SQL("SELECT MODIFIED, THREAD FROM THREAD_CACHE WHERE MODIFIED={modified}").on('modified -> params.key)().map {
          case Row(modified: Long, thread: Array[Byte]) =>
            Some(thread)
          case _ => None
        }
    }
    keys(0) match {
      case None =>
        Logger.error("no thread key in local database!"); Ok("dokogayanen")
      case Some(key) =>
        import org.apache.commons.codec.binary.Base64
        Logger.trace("thread key found.")
        val d = System.currentTimeMillis() / 1000
        val data = Response(key, params.FROM, params.mail, params.MESSAGE, d).toString.getBytes(/*"shift_jis"*/)
        val digestFactory = MessageDigest.getInstance("SHA-1")
        val digest = digestFactory.digest(data)

        val digestStr = Base64.encodeBase64(digest)
        Logger.debug(
          s"""
      |--- response information ---
      |Thread: $key
      |From: $params.FROM
      |Mail: $params.mail
      |MESSAGE: $params.MESSAGE
      |Digest(SHA-1): $digestStr
      """.stripMargin)

        Logger.debug("registering response information into Chord DHT...")
        val dht_keyO = Await.result(chord2ch.put(new String(Base64.encodeBase64(digest)), data.toStream), 30 second)
        Logger.debug("registered successfully.")

        dht_keyO match {
          case Some(dht_key) =>
            // put into h2db
            Logger.debug("registering response information into local database...")
            DB.withConnection {
              implicit c =>
                SQL("INSERT INTO RESPONSE_CACHE(THREAD, RESPONSE, MODIFIED) VALUES({thread}, {response}, {modified})").on(
                  'thread -> key,
                  'response -> dht_key.toArray[Byte],
                  'modified -> d).executeInsert()
            }
            Logger.info("response has written succesfully.")
            Ok(views.html.successfullyWritten()).as(HTML)
          case None =>
            Logger.error("response writing failed.")
            Ok(views.html.badlyWritten()).as(HTML)
        }

    }

  }

  private def buildThread(request: WriteRequestT) = {
    // put into chord
    import org.apache.commons.codec.binary.Base64
    Logger.info("building thread: " + request.subject)
    val d = System.currentTimeMillis() / 1000
    val data = Thread(request.subject, d, request.FROM, request.mail, request.MESSAGE).toString.getBytes(/*"shift_jis"*/)
    val digestFactory = MessageDigest.getInstance("SHA-1")
    val digest = digestFactory.digest(data)

    val digestStr = Base64.encodeBase64(digest)
    Logger.debug(
      s"""
       |--- Thread building information ---
       |Title: ${request.subject}
       |Epoch: $d
       |Digest(SHA-1): $digestStr
       |Name: ${request.subject}
       |Mail: ${request.mail}
       |Body: ${request.MESSAGE}
""".stripMargin)
    Logger.debug("registering thread data into Chord DHT...")
    val key: Option[Seq[Byte]] = Await.result(chord2ch.put(new String(Base64.encodeBase64(digest)), data.toStream).mapTo[Option[Seq[Byte]]], 30 second)
    Logger.debug("registered into Chord DHT.")

    Logger.debug("registering thread data into local database...")
    //put into h2db
    key match {
      case Some(dht_key) =>
        //println("key size: " + dht_key.size)
        DB.withConnection {
          implicit c =>
            SQL("INSERT INTO THREAD_CACHE(THREAD, MODIFIED) VALUES({thread}, {modified})").on(
              'thread -> dht_key.toArray[Byte],
              'modified -> d).executeInsert()
        }
        Logger.debug("thread information has registered successfully.")
        Ok(views.html.successfullyWritten()).as(HTML)
      case None =>
        Logger.error("building thread failed.")
        Ok(views.html.failedBuilding()).as(HTML)
    }
  }

  def searchResSince(epoch: Long) = {
    Logger.debug(s"searching responses uploaded since $epoch or more")
    DB.withConnection {
      implicit c =>
        SQL("SELECT RESPONSE, THREAD, MODIFIED FROM RESPONSE_CACHE WHERE MODIFIED >= {since}").on("since" -> epoch)().map {
          case Row(response: Array[Byte], thread: Array[Byte], modified: Long) =>
            ('newResponseResult, response, thread, modified)
        }.toList
    }
  }

  def searchThreadSince(epoch: Long) = {
    Logger.debug(s"searching thread built since $epoch or more")
    DB.withConnection {
      implicit c =>
        SQL("SELECT THREAD, MODIFIED FROM THREAD_CACHE WHERE MODIFIED >= {since}").on("since" -> epoch)().map {
          case Row(thread: Array[Byte], modified: Long) =>
            ('newThreadResult, thread, modified)
        }.toList
    }
  }

  def updateCache(nrr: List[NewResponseResult], ntr: List[NewThreadResult]) = {
    Logger.debug(s"updating thread/response local cache: thread(${ntr.size}), response(${nrr.size})")

    /*ignoring(classOf[Throwable])*/
    {
      // ignore unique fault
      DB.withConnection {
        implicit c =>
          nrr foreach {
            one =>
              SQL("INSERT INTO RESPONSE_CACHE(RESPONSE, THREAD, MODIFIED) VALUES ({response}, {thread}, {modified})").on(
                "response" -> one._2, // case classにするとなぜか使えないという苦肉
                "thread" -> one._3,
                "modified" -> one._4).executeInsert()
          }
          ntr foreach {
            one =>
              SQL("INSERT INTO THREAD_CACHE(THREAD, MODIFIED) VALUES ({address}, {modified})").on(
                "address" -> one._2,
                "modified" -> one._3).executeInsert()
          }
      }
    }
    Logger.debug("cache has updated successfully.")
  }

}

class ChordCore2ch extends ChordCore {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  var lastload: Long = 0
  val fetcher = context.actorOf(akka.actor.Props[DataFetchingBeacon], "FetchingBeacon")

  override def receiveExtension(x: Any, sender: ActorRef)(implicit context: ActorContext) = x match {
    case ('NewResSince, time: Long) => sender ! Application.searchResSince(time)
    case ('NewThreadSince, time: Long) => sender ! Application.searchThreadSince(time)
    case PullNew => pullNewData
  }

  override def init(id: nodeID) = {
    import context.dispatcher
    import scala.concurrent.duration._
    super.init(id)

    context.system.scheduler.schedule(30 seconds, 1 minutes, self, PullNew)
    fetcher !('start, self) // start beacon
  }

  override def postStop = {
    fetcher ! 'stop
  }

  def pullNewData = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import akka.pattern._
    import concurrent.duration._
    // ランダムなノードを選択し、newResSince/newThreadSinceをlastmodifiedに基づき発行。
    // データが返るのでそれをDBに反映する
    implicit val timeout: akka.util.Timeout = 30 second
    val randomly = new util.Random()
    val randomOne = randomly.shuffle(stateAgt().succList.nodes.list ++ stateAgt().fingerList.nodes.list).head
    val thatActor = randomOne.actorref
    val newResRslt = (a: ActorRef) => (sinceWhen: Long) => (a ?('NewResSince, sinceWhen)).mapTo[List[NewResponseResult]]
    val newThreadRslt = (a: ActorRef) => (sinceWhen: Long) => (a ?('NewThreadSince, sinceWhen)).mapTo[List[NewThreadResult]]
    val composedFuture = for {
      snts <- newThreadRslt(thatActor)(lastload)
      snrs <- newResRslt(thatActor)(lastload)
    } yield (snrs, snts)
    composedFuture.onSuccess {
      case (snrs, snts) =>
        Application.updateCache(snrs, snts)
        lastload = (System.currentTimeMillis() / 1000) - 3600000 // a hour ago
    }
  }
}

class Chord2ch extends Chord {

  import akka.actor._

  //val config = ConfigFactory.load()
  //val customConf = config.getConfig("p2pakka").withFallback({println("fallbacking"); config})
  /*val customConf = ConfigFactory.parseString(
    """
      loglevel = "DEBUG"
          actor {
              provider = "akka.remote.RemoteActorRefProvider"
          }
          remote {
              log-received-messages = on
              log-sent-messages = on
              transport = "akka.remote.netty.NettyRemoteTransport"
              netty {
                  hostname = "127.0.0.1"
                  port = 0
                  reuse-address = off-for-windows
                  }
              }
    """)*/
  //override val system = ActorSystem("P2P2CH", ConfigFactory.load(customConf))
  override val chord = system.actorOf(Props[ChordCore2ch], "ChordCore2ch")
}


object Global extends GlobalSettings {
  override def onStop(app: Application) {
    Application.stopping
  }
}

object Utility {
  def htmlEscape(s: String): String = {
    val s1: String = s.replaceAll("&", "&amp;");
    val s2: String = s1.replaceAll("<", "&lt;");
    val s3: String = s2.replaceAll(">", "&gt;");
    val s4: String = s3.replaceAll('"'.toString, "&quot;");
    s4.replaceAll("'", "&#039;")
  }

  def nanasify(name: String): String = name match {
    case "" => "P2Pの名無しさん"
    case s => s
  }
}

class DataFetchingBeacon extends akka.actor.Actor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz._
  import Scalaz._
  import akka.actor._

  var cancellable: Option[Cancellable] = None

  def receive = {
    case ('start, a: ActorRef) => startTimer(a)
    case 'stop => stopTimer
    case unknown =>
  }

  def startTimer(a: ActorRef) = {
    import scala.concurrent.duration._
    cancellable = context.system.scheduler.schedule(30 second, 15 second, a, PullNew).some
  }

  def stopTimer = {
    cancellable >>= (c => c.isCancelled match {
      case false => c.cancel().some
      case otherwise => None // do nothing
    })
    cancellable = None
  }
}

case object PullNew

case class Thread(title: String, since: Long, from: String, mail: String, body: String) {
  override def toString = {
    import Utility._
    val escapedBody: String = htmlEscape(body)
    htmlEscape(title) + "<>" + since + "<>" + htmlEscape(nanasify(from)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll("\n", " <br>")
  }
}

case class Response(thread: Array[Byte], name: String, mail: String, body: String, time: Long) {

  import org.apache.commons.codec.binary.Base64

  override def toString = {
    import Utility._
    val escapedBody: String = htmlEscape(body)
    new String(Base64.encodeBase64(thread)) + "<>" + htmlEscape(nanasify(name)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll("\n", " <br>") + "<>" + time
  }
}

//case class newThreadResult(address: Array[Byte], epoch: Long)
//case class newResponseResult(address: Array[Byte], threadAddress: Array[Byte], epoch: Long)
