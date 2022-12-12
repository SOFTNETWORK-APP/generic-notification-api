package app.softnetwork.notification.scalatest

import java.security.SecureRandom

trait ApnsToken {

  val TOKEN_LENGTH: Int = 32

  def generateRandomDeviceToken: String = {
    val tokenBytes = new Array[Byte](TOKEN_LENGTH)
    new SecureRandom().nextBytes(tokenBytes)
    val builder = new StringBuilder(TOKEN_LENGTH * 2)
    for (b <- tokenBytes) {
      builder.append("%02x".format(b))
    }
    builder.toString
  }

}
