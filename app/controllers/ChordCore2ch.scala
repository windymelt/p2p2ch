package controllers

import momijikawa.p2pscalaproto.{nodeID, ChordCore}
import akka.actor.{ActorContext, ActorRef}
import akka.agent.Agent

class ChordCore2ch extends ChordCore {
  type NewThreadResult = (Symbol, Array[Byte], Long)
  type NewResponseResult = (Symbol, Array[Byte], Array[Byte], Long)

  var lastload: Long = 0
  val fetcher = context.actorOf(akka.actor.Props[DataFetchingBeacon], "FetchingBeacon")

  override def receiveExtension(x: Any, sender: ActorRef)(implicit context: ActorContext) = x match {
    case ('NewResSince, time: Long) => sender ! Application.searchResSince(time)
    case ('NewThreadSince, time: Long) => sender ! Application.searchThreadSince(time)
    case PullNew => pullNewData
  }

  override def init(id: nodeID) = {
    import context.dispatcher
    import scala.concurrent.duration._
    super.init(id)

    context.system.scheduler.schedule(30 seconds, 1 minutes, self, PullNew)
    fetcher !('start, self) // start beacon
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
    val newResRslt = (a: ActorRef) => (sinceWhen: Long) => (a ?('NewResSince, sinceWhen)).mapTo[List[NewResponseResult]]
    val newThreadRslt = (a: ActorRef) => (sinceWhen: Long) => (a ?('NewThreadSince, sinceWhen)).mapTo[List[NewThreadResult]]
    val composedFuture = for {
      snts <- newThreadRslt(thatActor)(lastload)
      snrs <- newResRslt(thatActor)(lastload)
    } yield (snrs, snts)
    composedFuture.onSuccess {
      case (snrs, snts) =>
        Application.updateCache(snrs, snts)
        lastload = (System.currentTimeMillis() / 1000) - 3600 // a hour ago
    }
  }
}
