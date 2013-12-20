package controllers

import momijikawa.p2pscalaproto.Chord

class Chord2ch extends Chord {

  import akka.actor._

  override val receiver = system.actorOf(Props(classOf[Receiver2ch], stateAgt), "Receiver2ch")
}
