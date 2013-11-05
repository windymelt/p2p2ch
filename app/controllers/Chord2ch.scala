package controllers

import momijikawa.p2pscalaproto.Chord

class Chord2ch extends Chord {

  import akka.actor._

  override val chord = system.actorOf(Props[ChordCore2ch], "ChordCore2ch")
}
