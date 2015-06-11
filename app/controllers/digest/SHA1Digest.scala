package controllers.digest

import java.security.MessageDigest

object SHA1Digest extends Digest {
  def generateDigestFromByteArray(byteArr: Array[Byte]): Array[Byte] = {
    val digestFactory = MessageDigest.getInstance("SHA-1")
    digestFactory.digest(byteArr)
  }
}
