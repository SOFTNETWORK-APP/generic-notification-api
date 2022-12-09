package app.softnetwork.notification.spi

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.MailSettings
import app.softnetwork.persistence.typed._
import com.dumbster.smtp.SimpleSmtpServer
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SmtpMockServer extends StrictLogging {

  val name: String = "smtp"

  implicit def system: ActorSystem[_]

  lazy val smtpPort: Int = MailSettings.MailConfig.port

  def start(): Boolean = {
    Try(SimpleSmtpServer.start(smtpPort)) match {
      case Success(server) =>
        logger.info(s"Notification Mock Server $name started")
        implicit val classicSystem: _root_.akka.actor.ActorSystem = system
        val shutdown = CoordinatedShutdown(classicSystem)
        shutdown.addTask(
          CoordinatedShutdown.PhaseServiceRequestsDone,
          s"$name-graceful-terminate"
        ) { () =>
          logger.info(s"Stopping Notification Mock Server $name ...")
          server.stop()
          Future.successful(Done)
        }
        true
      case Failure(f) =>
        logger.error(f.getMessage, f)
        false
    }
  }
}

object SmtpMockServer {
  def apply(sys: ActorSystem[_]): SmtpMockServer = {
    new SmtpMockServer {
      override implicit def system: ActorSystem[_] = sys
    }
  }
}
