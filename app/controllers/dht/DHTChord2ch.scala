package controllers.dht

import akka.agent.Agent
import controllers.Chord2ch
import momijikawa.p2pscalaproto.TnodeID
import scala.concurrent.Future
import controllers.digest.Digest
import scala.util.{ Try, Success, Failure }
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

object DHTChord2ch extends DHT {
  val dht: Agent[Option[Chord2ch]] = Agent(Option[Chord2ch](null))
  def initialize(params: InitializeParams): Try[Boolean] = {
    val chord2ch = new Chord2ch()
    chord2ch.init(TnodeID.newNodeId)
    dht.alter(Some(chord2ch))
    Success(true)
  }
  def get(key: Key): Future[\/[DHTGetError, Value]] = {
    dht() match {
      case Some(chord2ch) ⇒
        chord2ch.get(key) flatMap {
          case Some(v) ⇒ Future(v.right)
          case None ⇒ Future(DHTNotFound.left)
        }
      case None ⇒ Future(DHTGetNotInitialized.left)
    }
  }
  def put(key: Key, value: Value): Future[\/[DHTPutError, Key]] = {
    dht() match {
      case Some(chord2ch) ⇒
        chord2ch.put(Digest.base64(key.toArray), value) flatMap {
          case Some(k: Seq[Byte]) ⇒
            Future(k.right)
          case None ⇒ Future(DHTPutUnknownError.left)
        }
      case None ⇒ Future(DHTPutNotInitialized.left)
    }
  }
  def join(param: JoinParam): Future[Boolean] = {
    dht() match {
      case Some(chord2ch) ⇒
        chord2ch.join(param) flatMap { _ ⇒ Future(true) }
      case None ⇒
        Future(false)
    }
  }
  def close(): Future[Boolean] = {
    dht() match {
      case Some(chord2ch) ⇒
        chord2ch.close()
        Future(true)
      case None ⇒
        Future(false)
    }
  }
  def identifier: Option[Identifier] = {
    dht() flatMap { _.getReference }
  }
  def info: Option[Information] = {
    dht() map { _.getStatus.toString }
  }
}
