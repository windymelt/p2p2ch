package controllers

import controllers.localdb.LocalDatabase
import momijikawa.p2pscalaproto.{ ChordState, nodeID, MessageReceiver }
import akka.actor.{ ActorContext, ActorRef }
import akka.agent.Agent

class Receiver2ch(stateAgt: Agent[ChordState]) extends MessageReceiver(stateAgt: Agent[ChordState]) {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  var lastload: Long = 0
  val fetcher = context.actorOf(akka.actor.Props[DataFetchingBeacon], "FetchingBeacon")

  override def receiveExtension(x: Any, sender: ActorRef)(implicit context: ActorContext) = x match {
    case ('NewResSince, time: Long) ⇒ sender ! LocalDatabase.default.getResponsesAfter(time)
    case ('NewThreadSince, time: Long) ⇒ sender ! LocalDatabase.default.getThreadsAfter(time)
    case PullNew ⇒ pullNewData
    case m ⇒ log.warning(s"unknown message: $m")
  }

  override def preStart = {
    import context.dispatcher
    import scala.concurrent.duration._
    context.system.scheduler.schedule(30 seconds, 1 minutes, self, PullNew)
    fetcher ! ('start, self) // start beacon
  }

  override def postStop = {
    fetcher ! 'stop
  }

  def pullNewData = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import akka.pattern._
    import concurrent.duration._
    // ランダムなノードを選択し、newResSince/newThreadSinceをlastmodifiedに基づき発行。
    // データが返るのでそれをDBに反映する
    implicit val timeout: akka.util.Timeout = 30 second
    val randomly = new util.Random()
    val randomOne = randomly.shuffle(this.stateAgt().succList.nodes.list ++ this.stateAgt().fingerList.nodes.list).head
    val thatActor = randomOne.actorref
    val newResRslt = (a: ActorRef) ⇒ (sinceWhen: Long) ⇒ (a ? ('NewResSince, sinceWhen)).mapTo[List[NewResponseResult]]
    val newThreadRslt = (a: ActorRef) ⇒ (sinceWhen: Long) ⇒ (a ? ('NewThreadSince, sinceWhen)).mapTo[List[NewThreadResult]]
    val composedFuture = for {
      newThreads ← newThreadRslt(thatActor)(lastload)
      newResponses ← newResRslt(thatActor)(lastload)
    } yield (newThreads, newResponses)
    composedFuture.onSuccess {
      case (threads, responses) ⇒
        CacheUpdater.updateLocalCache(threads, responses)
        lastload = (System.currentTimeMillis() / 1000) - 3600 // a hour ago
    }
  }
}
