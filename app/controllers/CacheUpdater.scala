package controllers

import controllers.localdb.LocalDatabase
import play.api.Logger
import scala.util.control.Exception.ignoring

object CacheUpdater {
  // TODO: case classに置換したい
  def updateLocalCache(
    threadUpdates: List[(Symbol, Array[Byte], Long)],
    responseUpdates: List[(Symbol, Array[Byte], Array[Byte], Long)]
  ) = {
    Logger.debug(s"updating local key table: ${threadUpdates.size} thread, ${responseUpdates.size} response")
    ignoring(classOf[Throwable]) { // レコードの重複で起こる例外を無視する
      threadUpdates foreach { one ⇒ LocalDatabase.default.insertThread(one._2, one._3) }
      responseUpdates foreach { one ⇒ LocalDatabase.default.insertResponse(one._3, one._2, one._4) }
    }
    Logger.debug("key table has updated successfully.")
  }
}
