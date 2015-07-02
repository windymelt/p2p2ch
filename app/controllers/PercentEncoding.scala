package controllers

import play.api.mvc.{ Headers, Request }

object PercentEncoding {
  import java.net.URI
  import scala.collection.JavaConversions._
  import org.apache.http.client.utils.URLEncodedUtils
  import org.apache.http.NameValuePair
  def extractSJISRequest(request: Request[String]) = {
    // パーセントエンコードされたStringが格納されているbodyを取得する。
    val body = request.body

    // ライブラリ独自のクラスであるNameValuePairのSeqに変換する。
    // URLEncodedUtils.parseはList<NameValuePair>を返すが、
    // scala.collection.JavaConversions._ をimportしているので
    // List(Java) -> Seq(Scala)に暗黙変換される。
    // URLEncodedUtilsは、本来はbodyではなくURL内での
    // パーセントエンコーディングを想定しているので、
    // ダミーURLを作成してそのクエリという体でパースさせる。
    val parsed: Seq[NameValuePair] = URLEncodedUtils.parse(new URI("http://example.com/?" + body), "Shift_JIS")

    // Seq[NameValuePair]をPlayでフォームデータの表現として使用される
    // Map[String, Seq[String]]に変換する。
    // 通常、application/x-www-form-urlencodedで渡ってきたデータは
    // Map[String, Seq[String]]となるが、ここで本来のデータ形式を再現する。
    val newBody = parsed.map { pair ⇒ (pair.getName, Seq(pair.getValue)) }.toMap

    // あたかも最初からフォームとしてパースされていたようなリクエストを用意する。
    // bodyだけが変更されており、その他のパラメータはオリジナルのまま。
    // 型もRequest[String]ではなくRequest[Map[String, Seq[String]]]になっている。
    new Request[Map[String, Seq[String]]] {
      override def body: Map[String, Seq[String]] = newBody // すげかえる

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
