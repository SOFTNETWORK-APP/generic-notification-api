package app.softnetwork.notification.scalatest

import akka.Done
import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.scalatest.MockServer
import app.softnetwork.notification.config.{InternalConfig, MailSettings}
import com.dumbster.smtp.SimpleSmtpServer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SmtpMockServer extends MockServer with MailSettings {
  _: InternalConfig =>

  val name: String = "smtp"

  implicit def system: ActorSystem[_]

  def serverPort: Int

  private[this] lazy val maybeServer: Option[SimpleSmtpServer] =
    Try(SimpleSmtpServer.start(serverPort)) match {
      case Success(server) => Some(server)
      case Failure(f) =>
        logger.error(s"Could not start mock server $name at $serverPort -> ${f.getMessage}")
        None
    }

  protected override def start(): Boolean = {
    maybeServer match {
      case Some(_) => true
      case _       => false
    }
  }

  protected override def stop(): Future[Done] = {
    maybeServer match {
      case Some(server) => server.stop()
      case _            =>
    }
    Future.successful(Done)
  }
}
