package models

class ListOfThreadHeaders(threadHeaders: Seq[ThreadHeader]) {
  def generateString: String = {
    views.html.threadList(threadHeaders).body
  }
}
