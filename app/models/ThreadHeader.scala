package models

case class ThreadHeader(title: String, since: Long, from: String, mail: String, body: String) {
  override def toString = {
    import controllers.Utility._
    val escapedBody: String = htmlEscape(body)
    htmlEscape(title) + "<>" + since + "<>" + htmlEscape(nanasify(from)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll(_BR_, " <br>")
  }
}

object ThreadHeader {
  import controllers.Utility
  def fromString(str: String): Option[ThreadHeader] = {
    val splitted = str.split("""<>""")
    if (splitted.length > 5) { return None }
    for (
      time ← Utility.string2LongOpt(splitted(1))
    ) yield ThreadHeader(title = splitted(0), since = time, from = splitted(2), mail = splitted(3), body = splitted(4))
  }
  def fromByteArray(arr: Array[Byte]): Option[ThreadHeader] = fromString(new String(arr, "utf-8"))
}
