package app.softnetwork.notification.scalatest

import akka.Done
import app.softnetwork.api.server.scalatest.MockServer
import app.softnetwork.notification.config.{ApnsConfig, InternalConfig, PushSettings}
import com.eatthepath.pushy.apns.server.{
  AcceptAllPushNotificationHandlerFactory,
  MockApnsServer,
  MockApnsServerBuilder,
  PushNotificationHandlerFactory
}

import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ApnsMockServer extends PushSettings with MockServer {
  _: InternalConfig =>

  override val name: String = "apns"

  lazy val apnsConfig: ApnsConfig = AppConfigs.getOrElse("mock", PushSettings.DefaultConfig).apns

  def serverPort: Int

  val SERVER_CERTIFICATES_FILENAME: String = "security/server-certs.pem"
  val SERVER_KEY_FILENAME: String = "security/server-key.pem"
  val SERVER_KEY_PASSWORD: Option[String] = None

  def handler: PushNotificationHandlerFactory = new AcceptAllPushNotificationHandlerFactory

  private[this] lazy val maybeServer: Option[MockApnsServer] =
    Try(
      new MockApnsServerBuilder()
        .setServerCredentials(
          getClass.getClassLoader.getResourceAsStream(SERVER_CERTIFICATES_FILENAME),
          getClass.getClassLoader.getResourceAsStream(SERVER_KEY_FILENAME),
          SERVER_KEY_PASSWORD.orNull
        )
        .setTrustedClientCertificateChain(
          getClass.getClassLoader.getResourceAsStream(apnsConfig.truststore.orNull)
        )
        .setHandlerFactory(handler)
        .build()
    ) match {
      case Success(value) => Some(value)
      case Failure(f) =>
        logger.error(s"Could not start mock server $name at $serverPort -> ${f.getMessage}")
        None
    }

  protected override def start(): Boolean = {
    maybeServer match {
      case Some(server) =>
        toScala(server.start(serverPort)) complete () match {
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

}
