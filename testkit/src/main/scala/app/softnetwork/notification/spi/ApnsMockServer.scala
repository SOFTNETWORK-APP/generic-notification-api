package app.softnetwork.notification.spi

import akka.Done
import app.softnetwork.notification.config.{ApnsConfig, PushSettings}
import com.eatthepath.pushy.apns.server.{
  AcceptAllPushNotificationHandlerFactory,
  MockApnsServer,
  MockApnsServerBuilder,
  PushNotificationHandlerFactory
}

import scala.compat.java8.FutureConverters._
import java.util.Random
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

trait ApnsMockServer extends NotificationMockServer { _: { def hostname: String } =>

  lazy val apnsConfig: ApnsConfig =
    PushSettings.AppConfigs.getOrElse("mock", PushSettings.DefaultConfig).apns

  lazy val apnsPort: Int = apnsConfig.port.getOrElse {
    import java.net.ServerSocket
    new ServerSocket(0).getLocalPort
  }

  val SERVER_CERTIFICATES_FILENAME: String = "security/server-certs.pem"
  val SERVER_KEY_FILENAME: String = "security/server-key.pem"

  def handler: PushNotificationHandlerFactory = new AcceptAllPushNotificationHandlerFactory

  private[this] lazy val maybeServer: Option[MockApnsServer] =
    Try(
      new MockApnsServerBuilder()
        .setServerCredentials(
          getClass.getClassLoader.getResourceAsStream(SERVER_CERTIFICATES_FILENAME),
          getClass.getClassLoader.getResourceAsStream(SERVER_KEY_FILENAME),
          null
        )
        .setTrustedClientCertificateChain(
          getClass.getClassLoader.getResourceAsStream(apnsConfig.truststore.orNull)
        )
        .setHandlerFactory(handler)
        .build()
    ) match {
      case Success(value) => Some(value)
      case Failure(f) =>
        logger.error(f.getMessage, f)
        None
    }

  override val name: String = "apns"

  protected override def start(): Boolean = {
    maybeServer match {
      case Some(server) =>
        toScala(server.start(apnsPort)) complete () match {
          case Success(value) => Option(value.toInt).isDefined
          case Failure(f) =>
            logger.error(f.getMessage, f)
            false
        }
      case None => false
    }
  }

  protected override def stop(): Future[Done] = {
    maybeServer match {
      case Some(server) => toScala(server.shutdown()) map (_ => Done)
      case _            => Future.successful(Done)
    }
  }

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
