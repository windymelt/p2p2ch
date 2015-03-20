package controllers.digest

import org.apache.commons.codec.binary.Base64

abstract class Digest {
  def generateDigestFromByteArray(byteArr: Array[Byte]): Array[Byte]
  def generateBase64DigestByteArrayFromByteArray(byteArr: Array[Byte]): Array[Byte] = Base64.encodeBase64(byteArr)
  def generateBase64DigestFromByteArray(byteArr: Array[Byte]): String = {
    new String(generateBase64DigestByteArrayFromByteArray(byteArr))
  }
}

object Digest {
  def default = SHA1Digest
}