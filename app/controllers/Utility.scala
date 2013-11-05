package controllers

object Utility {
  def htmlEscape(s: String): String = {
    val s1: String = s.replaceAll("&", "&amp;");
    val s2: String = s1.replaceAll("<", "&lt;");
    val s3: String = s2.replaceAll(">", "&gt;");
    val s4: String = s3.replaceAll('"'.toString, "&quot;");
    s4.replaceAll("'", "&#039;")
  }

  def nanasify(name: String): String = name match {
    case "" => "P2Pの名無しさん"
    case s => s
  }

  val Otimestamp2str: Option[Long] => String =
    (Otime: Option[Long]) => Otime.flatMap(t => Some(new java.util.Date(t * 1000).toString)).getOrElse("???")

  val stream_spliting = (resData: Stream[Byte]) => new String(resData.toArray[Byte]).split( """<>""").toSeq.toList

  def filterNotEmpty[T](lis: List[Option[T]]): List[T] = lis.collect {
    case Some(x) => x
  }


}
