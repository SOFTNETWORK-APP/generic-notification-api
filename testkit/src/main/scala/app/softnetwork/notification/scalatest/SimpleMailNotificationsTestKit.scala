package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  NotificationGrpcServerTestKit,
  NotificationServer,
  SimpleMailNotificationsServer
}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.SimpleMailNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  NotificationBehavior,
  SimpleMailNotificationsBehavior
}
import app.softnetwork.persistence.query.{InMemoryJournalProvider, InMemoryOffsetProvider}
import app.softnetwork.scheduler.config.SchedulerSettings
import com.typesafe.config.Config
import org.scalatest.Suite
import app.softnetwork.notification.model.Mail
import org.slf4j.{Logger, LoggerFactory}

trait SimpleMailNotificationsTestKit
    extends NotificationGrpcServerTestKit[Mail]
    with NotificationTestKit[Mail] { _: Suite =>

  lazy val smtpPort: Int = availablePort

  override lazy val additionalConfig: String = notificationGrpcConfig +
    s"""
       |notification.mail.host = $hostname
       |notification.mail.port = $smtpPort
       |""".stripMargin

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new SmtpMockServer with InternalConfig {
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = smtpPort

        override lazy val config: Config = internalConfig
      }.init()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Mail]] = _ =>
    Seq(
      new SimpleMailNotificationsBehavior with InternalConfig {
        override lazy val config: Config = internalConfig
        override def log: Logger = LoggerFactory.getLogger(this.getClass)
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with SimpleMailNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val tag: String =
            SchedulerSettings.tag(SimpleMailNotificationsBehavior.persistenceId)
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with SimpleMailNotificationsHandler
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
    system => Seq(SimpleMailNotificationsServer(system))

}
