package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  NotificationGrpcServerTestKit,
  NotificationServer,
  SMSModeNotificationsServer
}
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
import app.softnetwork.persistence.query.{InMemoryJournalProvider, InMemoryOffsetProvider}
import app.softnetwork.scheduler.config.SchedulerSettings
import com.typesafe.config.Config
import org.scalatest.Suite
import app.softnetwork.notification.model.SMS
import org.slf4j.{Logger, LoggerFactory}

trait SMSModeNotificationsTestKit
    extends NotificationGrpcServerTestKit[SMS]
    with NotificationTestKit[SMS] { _: Suite =>

  lazy val smsPort: Int = availablePort

  override lazy val additionalConfig: String = notificationGrpcConfig +
    s"""
       |notification.sms.mode.base-url = "http://$interface:$smsPort"
       |""".stripMargin

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new SMSMockServer with InternalConfig {
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = smsPort

        override def config: Config = internalConfig
      }.init()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[SMS]] = _ =>
    Seq(
      new SMSModeNotificationsBehavior with InternalConfig {
        lazy val config: Config = internalConfig
        override def log: Logger = LoggerFactory.getLogger(this.getClass)
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with SMSModeNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val tag: String =
            SchedulerSettings.tag(SMSModeNotificationsBehavior.persistenceId)
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with SMSModeNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(SMSModeNotificationsServer(system))
}
