package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  AllNotificationsServer,
  NotificationGrpcServerTestKit,
  NotificationServer
}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.AllNotificationsHandler
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  AllNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.notification.spi.FcmMockProvider
import app.softnetwork.persistence.query.{InMemoryJournalProvider, InMemoryOffsetProvider}
import app.softnetwork.scheduler.config.SchedulerSettings
import com.typesafe.config.Config
import org.scalatest.Suite
import org.slf4j.{Logger, LoggerFactory}

trait AllNotificationsTestKit
    extends NotificationGrpcServerTestKit[Notification]
    with NotificationTestKit[Notification]
    with ApnsToken {
  _: Suite =>

  lazy val apnsPort: Int = availablePort

  lazy val smsPort: Int = availablePort

  lazy val smtpPort: Int = availablePort

  lazy val allNotificationsHandler: AllNotificationsHandler = AllNotificationsHandler

  lazy val additionalNotificationConfig: String = notificationGrpcConfig +
    s"""
       |notification.mail.host = $hostname
       |notification.mail.port = $smtpPort
       |notification.push.mock.apns.port = $apnsPort
       |notification.sms.mode.base-url = "http://$interface:$smsPort"
       |""".stripMargin

  override lazy val additionalConfig: String = additionalNotificationConfig

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new ApnsMockServer with InternalConfig {
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = apnsPort

        override lazy val config: Config = internalConfig
      }.init()
    )
    assert(
      new SMSMockServer with InternalConfig {
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = smsPort

        override def config: Config = internalConfig
      }.init()
    )
    assert(
      new SmtpMockServer with InternalConfig {
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = smtpPort

        override lazy val config: Config = internalConfig
      }.init()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Notification]] =
    _ =>
      Seq(new AllNotificationsBehavior with FcmMockProvider with InternalConfig {
        override def config: Config = internalConfig
        override def log: Logger = LoggerFactory.getLogger(this.getClass)
      })

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with AllNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val tag: String = SchedulerSettings.tag(AllNotificationsBehavior.persistenceId)
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with AllNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] = system =>
    Seq(AllNotificationsServer(system))
}
