package controllers.dht

import java.net.UnknownHostException
import ow.dht.{ DHT ⇒ OWDHT, DHTConfiguration, DHTFactory }
import ow.id.ID
import ow.messaging.util.MessagingUtility
import ow.stat.StatConfiguration
import ow.stat.StatFactory
import scala.concurrent.Future
import akka.agent.Agent
import scala.util.{ Try, Success, Failure }
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

object DHTOverlayWeaver extends controllers.dht.DHT {
  type V = Array[Byte]
  val ID_SIZE = 20 /* bytes */
  val applicationID: Short = 22 /* P_2_P_2_ch */
  val applicationVersion: Short = 0
  lazy val dht: Agent[Option[OWDHT[V]]] = Agent(Option[OWDHT[V]](null))

  def initialize(params: InitializeParams): Try[Boolean] = {
    val conf: DHTConfiguration = new DHTConfiguration()
    dht.alter(Some(initializeDHT(applicationID = applicationID,
      applicationVersion = applicationVersion,
      config = conf,
      workingDir = None,
      transport = "UDP".some,
      algorithm = "Kademlia".some,
      routingStyle = "Iterative".some,
      selfID = ID.getRandomID(ID_SIZE).some,
      statCollectorAddressAndPort = None,
      selfAddressAndPort = None,
      noUPnP = false,
      contactHostAndPort = None)))
    Success(true)
  }
  def get(key: Key): Future[\/[DHTGetError, Value]] = dht() match {
    case Some(d) ⇒
      val v = d.get(ID.getID(key.toArray, ID_SIZE))
      v.iterator().hasNext match {
        case true  ⇒ Future(v.iterator().next().getValue.toStream.right)
        case false ⇒ Future(DHTNotFound.left)
      }
    case None ⇒ Future(DHTGetNotInitialized.left)
  }
  def put(key: Key, value: Value): Future[\/[DHTPutError, Key]] = dht() match {
    case Some(d) ⇒
      val result = d.put(ID.getID(key.toArray, ID_SIZE), value.toArray)
      Future(key.right)
    case None ⇒ Future(DHTPutUnknownError.left)
  }
  def join(joinTo: JoinParam): Future[Boolean] = dht() match {
    case Some(d) ⇒
      d.joinOverlay(joinTo)
      Future(true)
    case None ⇒ Future(false)
  }
  def close(): Future[Boolean] = dht() match {
    case Some(d) ⇒
      d.stop()
      Future(true)
    case None ⇒ Future(false)
  }
  def identifier: Option[Identifier] = dht() flatMap { _.getSelfIDAddressPair.toString.some }
  def info: Option[Information] = dht() flatMap { _.getConfiguration.toString.some }
  private def initializeDHT(applicationID: Short, applicationVersion: Short, config: DHTConfiguration,
                            workingDir: Option[String], transport: Option[String], algorithm: Option[String],
                            routingStyle: Option[String], selfID: Option[ID], statCollectorAddressAndPort: Option[String],
                            selfAddressAndPort: Option[String], noUPnP: Boolean = false, contactHostAndPort: Option[String]): OWDHT[V] = {
    var join = false

    contactHostAndPort foreach { _ ⇒ join = true }

    // initialize a DHT
    transport foreach config.setMessagingTransport
    algorithm foreach config.setRoutingAlgorithm
    routingStyle foreach config.setRoutingStyle
    workingDir foreach config.setWorkingDirectory
    selfAddressAndPort foreach {
      addrPort ⇒
        val hostAndPort = MessagingUtility.parseHostnameAndPort(addrPort, config.getSelfPort)
        config.setSelfAddress(hostAndPort.getHostName)
        config.setSelfPort(hostAndPort.getPort)
    }

    if (noUPnP) config.setDoUPnPNATTraversal(false)

    val dht = DHTFactory.getDHT[V](
      applicationID, applicationVersion, config, selfID | null) // throws Exception
    val sb = new StringBuilder()
    sb.append("DHT configuration:\n")
    sb.append(" hostname:port: ").append(dht.getSelfIDAddressPair.getAddress).append('\n')
    sb.append(" transport type: ").append(config.getMessagingTransport).append('\n')
    sb.append(" routing algorithm: ").append(config.getRoutingAlgorithm).append('\n')
    sb.append(" routing style: ").append(config.getRoutingStyle).append('\n')
    sb.append(" directory type: ").append(config.getDirectoryType).append('\n')
    sb.append(" working directory: ").append(config.getWorkingDirectory).append('\n')
    print(sb)
    try {
      statCollectorAddressAndPort foreach {
        addrPort ⇒
          val statConfig = StatFactory.getDefaultConfiguration
          // provides the default port number of stat collector
          val hostAndPort = MessagingUtility.parseHostnameAndPort(addrPort, statConfig.getSelfPort)
          dht.setStatCollectorAddress(hostAndPort.getHostName, hostAndPort.getPort)
      }

      if (join) {
        val HostAndPort = """([a-zA-Z.-]+):\d+""".r
        contactHostAndPort.get match {
          case HostAndPort(host, port) ⇒
            dht.joinOverlay(host, port.toInt)
          case hostandport ⇒
            try {
              dht.joinOverlay(hostandport)
            } catch { // port is not specified
              case e: IllegalArgumentException ⇒
                val contactPort = config.getContactPort
                dht.joinOverlay(hostandport, contactPort)
            }
        }
      }
    } catch {
      case e: UnknownHostException ⇒
        System.err.println("A hostname could not be resolved: " + contactHostAndPort)
        e.printStackTrace()
        System.exit(1)
    }
    if (join) {
      System.out.println(" initial contact: " + contactHostAndPort.get + s" (default port ${config.getContactPort}})")
    }
    System.out.println("A DHT started.")
    System.out.flush()
    dht
  }
}
