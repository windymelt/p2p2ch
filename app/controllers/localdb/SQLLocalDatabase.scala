package controllers.localdb

import anorm._
import play.api.db.DB
import play.api.Play.current // to get Application for DB access

object SQLLocalDatabase extends LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]] = {
    val threadKeyList = DB.withConnection {
      implicit c ⇒
        SQL("SELECT THREAD FROM THREAD_CACHE WHERE MODIFIED = {modified}").on("modified" -> datNumber)().map {
          case Row(thread: Array[Byte]) ⇒ Some(thread)
          case _                        ⇒ None
        }.filterNot(_.isEmpty).toSeq
    }
    threadKeyList(0)
  }
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): Seq[Array[Byte]] = {
    val responseKeys = DB.withConnection {
      implicit c ⇒
        SQL("SELECT RESPONSE, MODIFIED FROM RESPONSE_CACHE WHERE THREAD = {thread} ORDER BY MODIFIED").on("thread" -> threadKey)().map {
          case Row(response: Array[Byte], modified: Long) ⇒ response
        }.toSeq
    }
    responseKeys
  }
  def countResponsesIn(threadKey: Array[Byte]): Long = {
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT COUNT(RESPONSE) AS COUNT FROM RESPONSE_CACHE WHERE THREAD = {thread}").on("thread" -> threadKey)().map {
          case Row(count: Long) ⇒ count
        }.head
    }
  }
  def insertResponse(threadKey: Array[Byte], responseKey: Array[Byte], time: Long): Unit = {
    DB.withConnection {
      implicit c ⇒
        SQL("INSERT INTO RESPONSE_CACHE(THREAD, RESPONSE, MODIFIED) VALUES({thread}, {response}, {modified})").on(
          'thread -> threadKey,
          'response -> responseKey,
          'modified -> time).executeInsert()
    }
  }
  def insertThread(threadKey: Array[Byte], time: Long): Unit = {
    DB.withConnection {
      implicit c ⇒
        SQL("INSERT INTO THREAD_CACHE(THREAD, MODIFIED) VALUES({thread}, {modified})").on(
          'thread -> threadKey,
          'modified -> time).executeInsert()
    }
  }
  def getResponsesAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Array[Byte], Long)] = {
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT RESPONSE, THREAD, MODIFIED FROM RESPONSE_CACHE WHERE MODIFIED >= {since}").on("since" -> sinceUNIXTime)().map {
          case Row(response: Array[Byte], thread: Array[Byte], modified: Long) ⇒
            ('newResponseResult, response, thread, modified)
        }.toList
    }
  }
  def getThreadsAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Long)] = {
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT THREAD, MODIFIED FROM THREAD_CACHE WHERE MODIFIED >= {since}").on("since" -> sinceUNIXTime)().map {
          case Row(thread: Array[Byte], modified: Long) ⇒
            ('newThreadResult, thread, modified)
        }.toList
    }
  }
}
