package controllers.threadwriting

case class WriteRequestR(bbs: String,
                         key: Long,
                         time: String,
                         submit: String,
                         FROM: String,
                         mail: String,
                         MESSAGE: String)
