package controllers.dht

sealed trait DHTPutError {
  val description: String
}

object DHTPutNotInitialized extends DHTPutError {
  val description: String = "DHTが初期化されていません"
}

object DHTPutUnknownError extends DHTPutError {
  val description = "不明なエラーによりDHTにデータを挿入できませんでした。"
}