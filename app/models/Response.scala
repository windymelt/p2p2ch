package models

case class Response(thread: Array[Byte], name: String, mail: String, body: String, time: Long) {

  import org.apache.commons.codec.binary.Base64

  override def toString = {
    import controllers.Utility._
    val escapedBody: String = htmlEscape(body)
    new String(Base64.encodeBase64(thread)) + "<>" + htmlEscape(nanasify(name)) + "<>" + htmlEscape(mail) + "<>" + escapedBody.replaceAll(_BR_, " <br>") + "<>" + time
  }
}

object Response {

  import anorm._
  import org.apache.commons.codec.binary.Base64
  import play.api.Play.current
  import play.api.db._

  import scala.util.matching.Regex
  import scalaz.Scalaz._

  // レス番に対応する正規表現
  private def localMatcher = new Regex(""">>(\d{1,4})""", "resNumber")

  private def permanentMatcher = new Regex("""&gt;&gt;([a-zA-Z0-9=+/]+?)@""", "data")

  /**
   * レス番を永続化し、常に同じレスを参照するように加工します。
   * @param res ローカルなレスアンカーを利用しているResponse
   * @return レスアンカーが永続化されたResponse
   */
  def toPermanent(res: Response): Response = {
    val replacer: (Regex.Match) ⇒ String = {
      val intify: (Regex.Match) ⇒ Int = (_: Regex.Match).group("resNumber").toInt
      val DBInterrogate: (Int) ⇒ String = {
        (n: Int) ⇒
          n match {
            case 1 ⇒ ">>1"
            case m ⇒
              val decrN = m - 2 // >>1は別のDBに格納されているため除外する ofst0-. 1. ofst1. 2.
              DB.withConnection {
                implicit c ⇒
                  SQL(
                    """SELECT RESPONSE """ +
                      """FROM RESPONSE_CACHE WHERE THREAD = {thread} """ +
                      """ORDER BY MODIFIED ASC LIMIT 1 OFFSET {no}"""
                  ).on('thread → res.thread, 'no → decrN)().map {
                      case Row(response: Array[Byte]) ⇒ ">>" + new String(Base64.encodeBase64(response)) + "@"
                      case _                          ⇒ ">>" + m.toString
                    }.toList.headOption.getOrElse(">>" + m.toString)
              }
          }
      }
      intify >>> DBInterrogate
    }
    res.copy(body = localMatcher.replaceAllIn(res.body, replacer))
  }

  /**
   * 永続化された形式に変換されたレスアンカーを利用しているレスポンスをローカルなレスアンカーを仕様するレスポンスに変換します。
   * @param res 永続化形式でレスアンカーが記述されているレスポンス
   * @return レスアンカーがローカルな形式に変換されたレスポンス
   */
  def toLocalized(res: Response): Response = {
    val replacer: (Regex.Match) ⇒ String = {
      //play.api.Logger.info("replcr actv")
      val arrayfy: (Regex.Match) ⇒ Array[Byte] = (m: Regex.Match) ⇒ Base64.decodeBase64(m.group("data").getBytes())
      val DBInterrogate = {
        (arr: Array[Byte]) ⇒
          DB.withConnection {
            implicit c ⇒
              SQL(
                "SELECT ROWNUM() AS NUMBER, RESPONSE FROM ( " +
                  "SELECT THREAD, RESPONSE, MODIFIED " +
                  "FROM RESPONSE_CACHE " +
                  "WHERE THREAD = {thread} ORDER BY MODIFIED ASC" +
                  ")" /*whr res*/ ).on('thread → res.thread)().collect {
                  case n ⇒
                    val nn = n.asMap
                    //play.api.Logger.warn(nn.toString())
                    //play.api.Logger.warn((nn(".NUMBER").asInstanceOf[Int], new String(Base64.encodeBase64(nn("_0.RESPONSE").asInstanceOf[Option[Array[Byte]]].get))) toString())
                    (nn(".NUMBER").asInstanceOf[Int] + 1) → new String(Base64.encodeBase64(nn("_0.RESPONSE").asInstanceOf[Option[Array[Byte]]].get))
                }.toList.map {
                  pair ⇒ play.api.Logger.info(pair._2 + " vs. " + new String(Base64.encodeBase64(arr))); pair
                }.collect {
                  case (n: Int, res: String) if res == new String(Base64.encodeBase64(arr)) ⇒ s"&gt;&gt;$n"
                }.headOption.getOrElse("&gt;&gt;N/A")
          }
      }
      arrayfy >>> DBInterrogate
    }
    //play.api.Logger.info("Localizing: [" + res.body + "]matched in:" + permanentMatcher.findFirstMatchIn(res.body).map(_.group("data")))
    res.copy(body = permanentMatcher.replaceAllIn(res.body, replacer))
  }

}