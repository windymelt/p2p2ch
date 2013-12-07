package controllers

import momijikawa.p2pscalaproto.{TnodeID, nodeID}


object StatusGraph {

  import java.io.ByteArrayOutputStream
  import javax.imageio.ImageIO
  import java.awt._
  import java.awt.Graphics2D
  import java.awt.image.BufferedImage
  import momijikawa.p2pscalaproto.ChordState

  val HEIGHT: Int = 512
  val WIDTH: Int = 512

  def draw(action: (Graphics2D) => Unit) = {
    val bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
    val gp = bi.createGraphics()
    gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);

    action(gp)

    val st = new ByteArrayOutputStream()
    val ios = ImageIO.createImageOutputStream(st)
    val iw = ImageIO.getImageWritersByFormatName("png").next()
    iw.setOutput(ios)
    iw.write(bi)
    gp.dispose()

    st.toByteArray
  }

  def drawMap(ratio: Double, color: Color)(implicit gp: Graphics2D) = {
    gp.setColor(color)
    gp.drawOval(WIDTH * (1.0 - ratio) * 0.5 toInt, HEIGHT * (1.0 - ratio) * 0.5 toInt, WIDTH * ratio toInt, HEIGHT * ratio toInt)
  }

  def drawCircleOnMap(mapRatio: Double, rad: Double, r: Double, color: Color, fill: Boolean)(implicit gp: Graphics2D) = {
    gp.setColor(color)
    if (fill) {
      gp.fillOval(WIDTH / 2 + (WIDTH * Math.cos(rad) * (mapRatio * 0.5) - r * 0.5) toInt, HEIGHT / 2 + (HEIGHT * Math.sin(rad) * (mapRatio * 0.5) - r * 0.5) toInt, r toInt, r toInt)
    } else {
      gp.drawOval(WIDTH / 2 + (WIDTH * Math.cos(rad) * (mapRatio * 0.5) - r * 0.5) toInt, HEIGHT / 2 + (HEIGHT * Math.sin(rad) * (mapRatio * 0.5) - r * 0.5) toInt, r toInt, r toInt)
    }
  }

  def drawLineOnMap(mapRatio: Double, rad1: Double, rad2: Double, color: Color)(implicit gp: Graphics2D) = {
    gp.setColor(color)
    gp.drawLine(
      WIDTH / 2 + (WIDTH * Math.cos(rad1) * (mapRatio * 0.5)) toInt, HEIGHT / 2 + (HEIGHT * Math.sin(rad1) * (mapRatio * 0.5)) toInt,
      WIDTH / 2 + (WIDTH * Math.cos(rad2) * (mapRatio * 0.5)) toInt, HEIGHT / 2 + (HEIGHT * Math.sin(rad2) * (mapRatio * 0.5)) toInt
    )
  }

  def nodeID2rad(nid: TnodeID): Double = (BigInt.apply(1, nid.idVal) / (TnodeID.CHORDSIZE / 36000)).toDouble / 36000.0 * 2 * Math.PI

  def getStatusImage(st: ChordState): Array[Byte] = {
    val act = (gp: Graphics2D) => {
      gp.setStroke(new BasicStroke(1f))
      drawMap(0.8, Color.GRAY)(gp)
      drawMap(0.6, Color.DARK_GRAY)(gp)

      st.succList.nodes.list.foreach {
        ida =>
          drawLineOnMap(0.8, nodeID2rad(ida), nodeID2rad(st.selfID.get), Color.green)(gp)
          drawCircleOnMap(0.8, nodeID2rad(ida), (WIDTH + HEIGHT) / 2 * 0.01, Color.GREEN, false)(gp)
      }

      st.fingerList.nodes.list.foreach {
        ida =>
          drawLineOnMap(0.6, nodeID2rad(ida), nodeID2rad(st.selfID.get), Color.yellow)(gp)
          drawCircleOnMap(0.6, nodeID2rad(ida), (WIDTH + HEIGHT) / 2 * 0.01, Color.YELLOW, false)(gp)
      }

      st.pred.foreach {
        prd =>
          drawLineOnMap(0.8, nodeID2rad(prd), nodeID2rad(st.selfID.get), Color.magenta)(gp)
          drawCircleOnMap(0.8, nodeID2rad(prd), (WIDTH + HEIGHT) / 2 * 0.01, Color.MAGENTA, true)(gp)
      }
      drawCircleOnMap(0.8, nodeID2rad(st.selfID.get), (WIDTH + HEIGHT) / 2 * 0.02, Color.RED, true)(gp)
    }
    draw(act)
  }
}
