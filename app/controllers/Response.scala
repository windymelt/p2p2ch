package controllers

case class Response(thread: Array[Byte], name: String, mail: String, body: String, time: Long) {

  import org.apache.commons.codec.binary.Base64

  override def toString = {
    import Utility._
    val escapedBody: String = htmlEscape(body)
    new String(Base64.encodeBase64(thread)) + "<>" + htmlEscape(nanasify(name)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll("\n", " <br>") + "<>" + time
  }
}
