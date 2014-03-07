package controllers

import momijikawa.p2pscalaproto.ChordState

object Information {

  import play.api._
  import play.api.db._
  import play.api.Play.current
  import anorm._
  import Utility._

  def getInformation: String = {
    Logger.info("information has selected.")
    val str = "</b>INFO<b><><>INFORMATION<>このページにコマンドを書き込むことで各種の設定が可能です。\"help\"でコマンド一覧が表示されます。<>P2P2chの情報"
    val log = DB.withConnection {
      implicit c =>
        SQL("SELECT MESSAGE, MODIFIED FROM SETTING_LOG ORDER BY ID")().map {
          row => (row[String]("MESSAGE"), row[Option[Long]]("MODIFIED"))
        }.toList
    }.map {
      (t: (String, Option[Long])) => s"</b>INFO<b><>${Otimestamp2str(t._2)}<>INFORMATION<>${t._1}<>"
    }.mkString(_BR_)
    log match {
      case "" => str
      case s => str + _BR_ + log
    }
  }

  def configurate(message: String) = {
    val result = main_configuration(message)
    log(result)
  }

  def log(message: String) = {
    val timestamp = System.currentTimeMillis() / 1000
    DB.withConnection {
      implicit c =>
        SQL("INSERT INTO SETTING_LOG (MESSAGE, MODIFIED) VALUES ({message}, {now})").on("message" -> message, "now" -> timestamp).executeUpdate
    }
  }

  private def main_configuration(message: String): String = {
    import Application.chord2ch
    message.nonEmpty match {
      case true =>
        message.split(_BR_).toList match {
          case "help" :: Nil =>
            Seq(
              "join -- ノードと接続する",
              "Example:",
              "join",
              "nodeID",
              "references",
              "",
              "reference -- ノードの接続キーを表示する",
              "",
              "upload (path) -- ファイルをアップロードする",
              "",
              "help -- このコマンドを表示する"
            ).mkString("<br>")
          case "reference" :: Nil => chord2ch.getReference.getOrElse("N/A")
          case "join" :: reference :: Nil =>
            chord2ch.join(reference); "接続を試行します"
          case "status" :: Nil =>
            val cst: ChordState = chord2ch.getStatus
            Seq(
              s"Self: ${
                cst.selfID.map {
                  _.getNodeID
                }.getOrElse("N/A")
              }",
              "Succ:",
              s"${cst.succList.nodes.list.mkString("<br>")}",
              "",
              "Finger:",
              s"${cst.fingerList.nodes.list.mkString("<br>")}",
              "",
              s"Pred: ${
                cst.pred.map {
                  _.getNodeID
                }.getOrElse("N/A")
              }",
              s"Data: ${cst.dataholder.size}",
              s"Stabilizer: ${cst.stabilizer.status}"
            ).mkString("<br>")
          //.toString.replace("\n", "<br>")
          case upload :: Nil => upload.split(" ").toList match {
            case "upload" :: path :: Nil => "まだ実装されてない"
            case _ => "そんなコマンド知らん"
          }
          case _ => "そんなコマンド知らん"
        }
      case false => "空白は困ります"
    }
  }
}
