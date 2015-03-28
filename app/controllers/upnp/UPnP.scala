package controllers.upnp

import scala.concurrent.duration.FiniteDuration

abstract class UPnP(val external_port: Int,
                    val local_port: Int,
                    val protocol: String,
                    val description: String,
                    val limit: FiniteDuration) {
  def open(): Boolean
  def close(): Boolean
  def externalAddress: String
}

object UPnP {
  def default(external_port: Int, local_port: Int, protocol: String, description: String, limit: FiniteDuration): UPnP =
    new UPnPPsyonik(external_port, local_port, protocol, description, limit)
}
