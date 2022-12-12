package app.softnetwork.notification.spi

import java.util.Random

trait ApnsToken {

  val TOKEN_LENGTH: Int = 32

  def generateRandomDeviceToken: String = {
    val tokenBytes = new Array[Byte](TOKEN_LENGTH)
    new Random().nextBytes(tokenBytes)
    val builder = new StringBuilder(TOKEN_LENGTH * 2)
    for (b <- tokenBytes) {
      builder.append("%02x".format(b))
    }
    builder.toString
  }

}
