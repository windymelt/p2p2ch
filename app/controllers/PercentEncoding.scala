package controllers

import play.api.mvc.{ Headers, Request }

object PercentEncoding {
  import java.net.URI
  import scala.collection.JavaConversions._
  import org.apache.http.client.utils.URLEncodedUtils
  import org.apache.http.NameValuePair
  def extractSJISRequest(request: Request[String]) = {
    val body = request.body
    val parsed: Seq[NameValuePair] = URLEncodedUtils.parse(new URI("http://example.com/?" + body), "Shift_JIS")
    val newBody = parsed.map { pair ⇒ (pair.getName, Seq(pair.getValue)) }.toMap
    new Request[Map[String, Seq[String]]] {
      override def body: Map[String, Seq[String]] = newBody

      override def uri: String = request.uri

      override def remoteAddress: String = request.remoteAddress

      override def queryString: Map[String, Seq[String]] = request.queryString

      override def method: String = request.method

      override def headers: Headers = request.headers

      override def path: String = request.path

      override def version: String = request.version

      override def tags: Map[String, String] = request.tags

      override def id: Long = request.id
    }
  }
}
