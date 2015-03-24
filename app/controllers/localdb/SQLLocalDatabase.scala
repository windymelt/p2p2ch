package controllers.localdb

import anorm._
import play.api.db.DB
import play.api.Play.current // to get Application for DB access
import play.api.Logger

object SQLLocalDatabase extends LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]] = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.fetchThreadKeyFromDatNumber called.")
    val threadKeyList = DB.withConnection {
      implicit c ⇒
        SQL("SELECT THREAD FROM THREAD_CACHE WHERE MODIFIED = {modified}").on("modified" -> datNumber)().map {
          case Row(thread: Array[Byte]) ⇒ Some(thread)
          case _                        ⇒ None
        }.filterNot(_.isEmpty).toList
    }
    threadKeyList(0)
  }
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): List[Array[Byte]] = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.fetchResponseKeysFromThreadKey called.")
    val responseKeys = DB.withConnection {
      implicit c ⇒
        Logger.trace("inner")
        SQL("SELECT RESPONSE, MODIFIED FROM RESPONSE_CACHE WHERE THREAD = {thread} ORDER BY MODIFIED").on("thread" -> threadKey)().map {
          case Row(response: Array[Byte], modified: Long) ⇒
            response
        }.toList
    }
    responseKeys
  }
  def countResponsesIn(threadKey: Array[Byte]): Long = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.countResponseIn called.")
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT COUNT(RESPONSE) AS COUNT FROM RESPONSE_CACHE WHERE THREAD = {thread}").on("thread" -> threadKey)().map {
          case Row(count: Long) ⇒ count
        }.head
    }
  }
  def insertResponse(threadKey: Array[Byte], responseKey: Array[Byte], time: Long): Unit = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.insertResponse called.")
    DB.withConnection {
      implicit c ⇒
        SQL("INSERT INTO RESPONSE_CACHE(THREAD, RESPONSE, MODIFIED) VALUES({thread}, {response}, {modified})").on(
          'thread -> threadKey,
          'response -> responseKey,
          'modified -> time).executeInsert()
    }
  }
  def insertThread(threadKey: Array[Byte], time: Long): Unit = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.insertThread called.")
    DB.withConnection {
      implicit c ⇒
        SQL("INSERT INTO THREAD_CACHE(THREAD, MODIFIED) VALUES({thread}, {modified})").on(
          'thread -> threadKey,
          'modified -> time).executeInsert()
    }
  }
  def getResponsesAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Array[Byte], Long)] = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.getResponseAfter called.")
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT RESPONSE, THREAD, MODIFIED FROM RESPONSE_CACHE WHERE MODIFIED >= {since}").on("since" -> sinceUNIXTime)().map {
          case Row(response: Array[Byte], thread: Array[Byte], modified: Long) ⇒
            ('newResponseResult, response, thread, modified)
        }.toList
    }
  }
  def getThreadsAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Long)] = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.getThreadsAfter called.")
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT THREAD, MODIFIED FROM THREAD_CACHE WHERE MODIFIED >= {since}").on("since" -> sinceUNIXTime)().map {
          case Row(thread: Array[Byte], modified: Long) ⇒
            ('newThreadResult, thread, modified)
        }.toList
    }
  }
  def getThreads: List[Array[Byte]] = {
    Logger.trace("controllers.localdb.SQLLocalDatabase.getThreads called.")
    DB.withConnection {
      implicit c ⇒
        SQL("SELECT THREAD FROM THREAD_CACHE")().map {
          case Row(thread: Array[Byte]) ⇒ thread
        }.toList
    }
  }
}
