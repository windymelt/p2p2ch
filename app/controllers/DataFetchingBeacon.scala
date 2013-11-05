package controllers

class DataFetchingBeacon extends akka.actor.Actor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalaz._
  import Scalaz._
  import akka.actor._

  var cancellable: Option[Cancellable] = None

  def receive = {
    case ('start, a: ActorRef) => startTimer(a)
    case 'stop => stopTimer
    case unknown =>
  }

  def startTimer(a: ActorRef) = {
    import scala.concurrent.duration._
    cancellable = context.system.scheduler.schedule(30 second, 15 second, a, PullNew).some
  }

  def stopTimer = {
    cancellable >>= (c => c.isCancelled match {
      case false => c.cancel().some
      case otherwise => None // do nothing
    })
    cancellable = None
  }
}
