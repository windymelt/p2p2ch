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
import java.security.MessageDigest
import play.api.libs.iteratee.Enumerator
import play.api.libs.concurrent.Promise
import play.api.libs.Comet

object Application extends Controller {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  import scala.concurrent.Await
  import scala.concurrent.duration._
  import controllers.Utility._

  implicit val myCustomCharset = Codec.javaSupported("Shift_JIS")
  val chord2ch = new Chord2ch
  chord2ch.init(TnodeID.newNodeId)

  def stopping() = {
    chord2ch.close()
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def bbsindex = Action {
    Ok(views.html.bbsindex("test")).as(HTML)
  }

  def bbsmenu = Action {
    Ok(views.html.bbsmenu()).as(HTML)
  }

  def bbstable = Action {
    Ok(views.html.bbstable()).as(HTML)
  }

  def subject = Action {
    Ok(Subject.generateSubject(chord2ch).getBytes("shift_jis")).as("text/plain")
  }

  def information = Action {
    Ok(Information.getInformation.getBytes("shift_jis")).as("text/plain")
  }

  def config_information(from: String, mail: String, message: String) = {
    Information.configurate(message)
    Ok(views.html.successfullyWritten()).as(HTML)
  }

  def showStatusImage = Action {
    Ok(StatusGraph.getStatusImage(chord2ch.getStatus)).as("image/png").withHeaders("Cache-Conrol" -> "no-cache")
  }

  def showStatusImageWithRefresh(interval: Int = 30) = Action {
    Ok(views.html.statusImageWithRefresh(interval)).as(HTML)
  }

  def showThread(dat: String) = Action {
    //datStrをLongに変換しキャッシュdbから呼び、複数候補が返るので0番目を選択、Optionをcopure。
    //さらに対応するresponseをldbから呼ぶ（ここは結合でまとめておく）。
    import scala.util.control.Exception._
    import org.apache.commons.codec.binary.Base64
    Logger.info(s"loading thread: $dat")
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
          /*}.filterNot {
            _.isEmpty*/
        }.flatten.collect {
          case Some(v: Stream[Byte]) =>
            val splited = new String(v.toArray).split("<>")
            Response.toLocalized(Response(Base64.decodeBase64(splited(0).getBytes), splited(1), splited(2), splited(3), splited(4).toLong))
        }
        Ok(
          views.html.thread(List((threadC.from, threadC.mail, Otimestamp2str(Some(threadC.since)), threadC.body, threadC.title)) ++
            responses.map {
              ar => (ar.name, ar.mail, Otimestamp2str(Some(ar.time)), ar.body, "")
            }.toList).body.getBytes("shift_jis")).as("text/plain").withHeaders("Cache-Conrol" -> "no-cache")

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
      Logger.info(s"request encoding is: ${request.charset}")

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
          buildThread( /*strMalSJIS2strU(*/ params /*params.subject*/ ) //)

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
    keys.headOption.getOrElse(None) match {
      case None =>
        Logger.error("no thread key in local database!"); Ok("dokogayanen")
      case Some(key) =>
        import org.apache.commons.codec.binary.Base64
        Logger.trace("thread key found.")

        val responseExceedsLimit: Boolean = {
          DB.withConnection {
            implicit c =>
              SQL("SELECT COUNT(RESPONSE) AS COUNT FROM RESPONSE_CACHE WHERE THREAD = {thread}").on("thread" -> key)().map {
                case Row(count: Long) => count
              }.head >= 999 // >>1はカウントされないため
          }
        }

        if (responseExceedsLimit) {
          Ok("このスレッドには書き込めません！(OVERRUN)")
        } else {
          val d = System.currentTimeMillis() / 1000
          val data = Response.toPermanent(Response(key, params.FROM, params.mail, params.MESSAGE, d)).toString.getBytes( /*"shift_jis"*/ )
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
  }

  private def buildThread(request: WriteRequestT) = {
    // put into chord
    import org.apache.commons.codec.binary.Base64
    Logger.info("building thread: " + request.subject)
    val d = System.currentTimeMillis() / 1000
    val data = Thread(request.subject.replace(_BR_, ""), d, request.FROM.replace(_BR_, ""), request.mail.replace(_BR_, ""), request.MESSAGE).toString.getBytes( /*"shift_jis"*/ )
    val digestFactory = MessageDigest.getInstance("SHA-1")
    val digest = digestFactory.digest(data)

    val digestStr = Base64.encodeBase64(digest)
    Logger.debug(
      s"""
       |--- Thread building information ---
       |Title: ${request.subject}
       |Epoch: $d
       |Digest(SHA-1): $digestStr
       |Name: ${request.FROM}
       |Mail: ${request.mail}
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
    import scala.util.control.Exception.ignoring
    Logger.debug(s"updating thread/response local cache: thread(${ntr.size}), response(${nrr.size})")

    nrr foreach {
      one =>
        // ignore unique fault
        ignoring(classOf[Throwable]) {
          DB.withConnection {
            implicit c =>
              SQL("INSERT INTO RESPONSE_CACHE(RESPONSE, THREAD, MODIFIED) VALUES ({response}, {thread}, {modified})").on(
                "response" -> one._2, // case classにするとなぜか使えないという苦肉
                "thread" -> one._3,
                "modified" -> one._4).executeInsert()
          }
        }
        ntr foreach {
          one =>
            ignoring(classOf[Throwable]) {
              DB.withConnection {
                implicit c =>
                  SQL("INSERT INTO THREAD_CACHE(THREAD, MODIFIED) VALUES ({address}, {modified})").on(
                    "address" -> one._2,
                    "modified" -> one._3).executeInsert()
              }
            }
        }
    }
    Logger.debug("cache has updated successfully.")
  }

  def statusGraphRealtime = Action {
    Ok(views.html.statusImageRealtime()).as(HTML).withHeaders("Cache-Conrol" -> "no-cache")
  }

  def statusGraphComet = Action {
    request =>
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

