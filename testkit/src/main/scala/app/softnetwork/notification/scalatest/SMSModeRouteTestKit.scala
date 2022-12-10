package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.Route
import app.softnetwork.notification.api.{NotificationServer, SMSModeNotificationsServer}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.SMSModeNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  NotificationBehavior,
  SMSModeNotificationsBehavior
}
import app.softnetwork.notification.spi.SMSModeService
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite
import org.softnetwork.notification.model.SMS

trait SMSModeRouteTestKit extends NotificationRouteTestKit[SMS] { _: Suite =>

  override lazy val additionalConfig: String = grpcConfig +
    s"""
      |notification.sms.mode.base-url = http://$interface:$port
      |""".stripMargin

  override def apiRoutes(system: ActorSystem[_]): Route = SMSModeService.route

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[SMS]] = _ =>
    Seq(
      new SMSModeNotificationsBehavior with InternalConfig {
        lazy val config: Config = akkaConfig.withFallback(ConfigFactory.load())
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with SMSModeNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${SMSModeNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with SMSModeNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(SMSModeNotificationsServer(system))
}
