package controllers.threadbuilding

sealed trait ThreadBuildingFailed {
  val message: String
}
object DHTPutFailed extends ThreadBuildingFailed {
  val message = "DHTへの書き込みに失敗しました。"
}