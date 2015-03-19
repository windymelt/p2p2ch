package controllers

case class ThreadHeader(title: String, since: Long, from: String, mail: String, body: String) {
  override def toString = {
    import Utility._
    val escapedBody: String = htmlEscape(body)
    htmlEscape(title) + "<>" + since + "<>" + htmlEscape(nanasify(from)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll(_BR_, " <br>")
  }
}
