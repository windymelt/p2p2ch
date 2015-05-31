package controllers.upnp

import com.psyonik.upnp._
import scala.concurrent.duration.FiniteDuration
import play.api.Logger

class UPnPPsyonik(override val external_port: Int,
                  override val local_port: Int,
                  override val protocol: String,
                  override val description: String,
                  override val limit: FiniteDuration) extends UPnP(external_port, local_port, protocol, description, limit) {
  def open(): Boolean = GatewayDiscover().getValidGateway() exists { gw: GatewayDevice ⇒
    Logger.debug("UPnP: found Gateway")
    val lcl_addr = gw.localAddress
    val ext_addr = gw.externalIPAddress
    gw.getSpecificPortMappingEntry(external_port, protocol) match {
      case Some(_) ⇒
        Logger.warn("UPnP: port mapping already exists")
        false
      case None ⇒
        Logger.debug("requesting port mapping...")
        gw.addPortMapping(external_port, local_port, lcl_addr.getHostAddress, protocol, description, limit.toSeconds.toInt)
    }
  }
  def close(): Boolean = GatewayDiscover().getValidGateway() exists { gw: GatewayDevice ⇒
    gw.getSpecificPortMappingEntry(external_port, protocol) match {
      case Some(_) ⇒ gw.deletePortMapping(external_port, protocol)
      case None    ⇒ false
    }
  }
}

object UPnPPsyonik {
  def externalAddress: Option[String] = {
    Logger.info("extn addr")
    val gateways = GatewayDiscover()
    Logger.info(gateways.size + " gateway(s) found")
    Logger.info(gateways.getValidGateway().toString)
    GatewayDiscover().getValidGateway() flatMap {
      gw ⇒
        Logger.info("valid g/w")
        Logger.info("g/w CNCT: " + gw.isConnected)
        Logger.info("local addr: " + gw.localAddress.toString)
        gw.externalIPAddress
    }
  }
}