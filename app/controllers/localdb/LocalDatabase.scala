package controllers.localdb

abstract class LocalDatabase {
  def fetchThreadKeyFromDatNumber(datNumber: Long): Option[Array[Byte]]
  def fetchResponseKeysFromThreadKey(threadKey: Array[Byte]): Seq[Array[Byte]]
}
