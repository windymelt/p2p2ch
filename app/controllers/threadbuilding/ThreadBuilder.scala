package controllers.threadbuilding

import controllers.dht.DHT
import controllers.Utility._
import controllers.digest.Digest
import controllers.localdb.LocalDatabase
import models.ThreadHeader
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

class ThreadBuilder {
  def buildThread(subject: String, from: String, mail: String, message: String): \/[ThreadBuildingFailed, ThreadBuildingSuccess.type] = {
    // put into chord
    Logger.info("building thread: " + subject)
    val currentUNIXTime = System.currentTimeMillis() / 1000
    val data = ThreadHeader(subject.replace(_BR_, ""), currentUNIXTime, from.replace(_BR_, ""), mail.replace(_BR_, ""), message).toString.getBytes( /*"shift_jis"*/ )
    val digest = Digest.default.generateDigestFromByteArray(data)

    Logger.debug(
      s"""
         |--- Thread building information ---
         |Title: $subject
         |Epoch: $currentUNIXTime
         |Digest(SHA-1): ${Digest.default.generateBase64DigestFromByteArray(data)}
         |Name: $from
         |Mail: $mail
""".stripMargin
    )

    Logger.debug("registering thread data into Chord DHT...")
    val key: Option[Seq[Byte]] = Await.result(DHT.default.put(digest, data.toStream), 30 seconds).toOption
    Logger.debug("registered into Chord DHT.")

    Logger.debug("registering thread data into local database...")
    key match {
      case Some(dht_key) ⇒
        LocalDatabase.default.insertThread(dht_key.toArray, currentUNIXTime)
        Logger.debug("thread information has registered successfully.")
        ThreadBuildingSuccess.right
      case None ⇒
        Logger.error("building thread failed.")
        DHTPutFailed.left
    }
  }
}
