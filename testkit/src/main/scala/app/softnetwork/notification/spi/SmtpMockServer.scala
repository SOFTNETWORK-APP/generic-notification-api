package app.softnetwork.notification.spi

import akka.Done
import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{InternalConfig, MailSettings}
import com.dumbster.smtp.SimpleSmtpServer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SmtpMockServer extends NotificationMockServer with MailSettings { _: InternalConfig =>

  val name: String = "smtp"

  implicit def system: ActorSystem[_]

  lazy val smtpPort: Int = MailConfig.port

  private[this] lazy val maybeServer: Option[SimpleSmtpServer] =
    Try(SimpleSmtpServer.start(smtpPort)) match {
      case Success(server) => Some(server)
      case Failure(f) =>
        logger.error(f.getMessage, f)
        None
    }

  override def start(): Boolean = {
    maybeServer match {
      case Some(_) => true
      case _       => false
    }
  }

  override def stop(): Future[Done] = {
    maybeServer match {
      case Some(server) => server.stop()
      case _            =>
    }
    Future.successful(Done)
  }
}
