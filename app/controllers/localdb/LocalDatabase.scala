package controllers.localdb

abstract class LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]]
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): Seq[Array[Byte]]
  def countResponsesIn(threadKey: Array[Byte]): Long
  def insertResponse(threadKey: Array[Byte], responseKey: Array[Byte], time: Long): Unit
}

object LocalDatabase {
  def default = SQLLocalDatabase
}