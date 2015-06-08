package controllers.localdb

abstract class LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]]
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): Seq[Array[Byte]]
  def countResponsesIn(threadKey: Array[Byte]): Long
  def insertResponse(threadKey: Array[Byte], responseKey: Array[Byte], time: Long): Unit
  def insertThread(threadKey: Array[Byte], time: Long): Unit
  def getResponsesAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Array[Byte], Long)]
  def getThreadsAfter(sinceUNIXTime: Long): List[(Symbol, Array[Byte], Long)]
  def getThreads: List[Array[Byte]]
}

object LocalDatabase {
  def default = SQLLocalDatabase
}