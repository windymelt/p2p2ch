package controllers.localdb

import anorm._
import play.api.db.DB
import play.api.Play.current // to get Application for DB access

object SQLLocalDatabase extends LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]] = {
    val threadKeyList = DB.withConnection {
      implicit c =>
        SQL("SELECT THREAD FROM THREAD_CACHE WHERE MODIFIED = {modified}").on("modified" -> datNumber)().map {
          case Row(thread: Array[Byte]) => Some(thread)
          case _ => None
        }.filterNot(_.isEmpty).toSeq
    }
    threadKeyList(0)
  }
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): Seq[Array[Byte]] = {
    val responseKeys = DB.withConnection {
      implicit c =>
        SQL("SELECT RESPONSE, MODIFIED FROM RESPONSE_CACHE WHERE THREAD = {thread} ORDER BY MODIFIED").on("thread" -> threadKey)().map {
          case Row(response: Array[Byte], modified: Long) => response
        }.toSeq
    }
    responseKeys
  }
}
