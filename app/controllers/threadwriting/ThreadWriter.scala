package controllers.threadwriting

import controllers.Application._
import controllers.digest.Digest
import controllers.localdb.LocalDatabase
import models.Response
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

class ThreadWriter {
  def writeThread(datNumber: Long, from: String, mail: String, message: String): \/[ThreadWritingFailed, ThreadWritingSuccess.type] = {
    // すれたてるときにcurrenttimemilの衝突がおこるかも
    // put into chord
    //keyに対応するthreadを取得する
    Logger.info("writing response.")
    Logger.debug("checking whether there's thread key in local database.")
    val threadKeyOpt = LocalDatabase.default.fetchThreadKeyFromDatNumber(datNumber)

    threadKeyOpt match {
      case None ⇒
        ThreadNotFoundInLocalDatabase.left
      case Some(key) ⇒
        Logger.trace("thread key found.")

        val responseExceedsLimit: Boolean = LocalDatabase.default.countResponsesIn(key) >= 999 // >>1はカウントされないため

        if (responseExceedsLimit) { return ThreadOverRun.left }

        val currentUNIXTime = System.currentTimeMillis() / 1000
        val data = Response.toPermanent(Response(key, from, mail, message, currentUNIXTime)).toString.getBytes( /*"shift_jis"*/ )

        val digestBase64 = Digest.default.generateBase64DigestFromByteArray(data)
        Logger.debug(
          s"""
             |--- response information ---
             |Thread: $key
              |From: $from
              |Mail: $mail
              |MESSAGE: $message
              |Digest(SHA-1): $digestBase64
            """.stripMargin)

        Logger.debug("registering response information into Chord DHT...")
        val dht_keyO = Await.result(chord2ch.put(digestBase64, data.toStream), 30 seconds)
        Logger.debug("registered successfully.")

        dht_keyO match {
          case Some(dht_key) ⇒
            // put into h2db
            Logger.debug("registering response information into local database...")
            LocalDatabase.default.insertResponse(key, dht_key.toArray, currentUNIXTime)
            Logger.info("response has written succesfully.")
            ThreadWritingSuccess.right
          case None ⇒
            Logger.error("response writing failed.")
            DHTPutFailed.left
        }
    }

  }
}
