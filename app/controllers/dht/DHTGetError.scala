package controllers.dht

sealed trait DHTGetError {
  val description: String
}

object DHTGetNotInitialized extends DHTGetError {
  val description: String = "DHTが初期化されていません"
}

object DHTNotFound extends DHTGetError {
  val description: String = "キーに対応する値を見付けられませんでした"
}