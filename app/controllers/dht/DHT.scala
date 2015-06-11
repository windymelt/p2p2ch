package controllers.dht

import scala.concurrent.Future
import scala.util.Try
import scalaz._
import Scalaz._

abstract class DHT {
  type Key = Seq[Byte]
  type Value = Stream[Byte]
  type InitializeParams = String
  type JoinParam = String
  type Identifier = String
  type Information = String

  def initialize(params: InitializeParams): Try[Boolean]
  def get(key: Key): Future[\/[DHTGetError, Value]]
  def put(key: Key, value: Value): Future[\/[DHTPutError, Key]]
  def join(joinTo: JoinParam): Future[Boolean]
  def close(): Future[Boolean]
  def identifier: Option[Identifier]
  def info: Option[Information]
}

object DHT {
  def default: DHT = DHTChord2ch
}