package controllers.threadwriting

sealed trait ThreadWritingFailed {
  val message: String
}
object ThreadNotFoundInLocalDatabase extends ThreadWritingFailed {
  val message = "ローカルデータベースに該当するスレッドの情報がありません。"
}
object ThreadOverRun extends ThreadWritingFailed {
  val message = "スレッドの最大書き込み数を超過しています。"
}
object DHTPutFailed extends ThreadWritingFailed {
  val message = "DHTへの書き込みに失敗しました。"
}
