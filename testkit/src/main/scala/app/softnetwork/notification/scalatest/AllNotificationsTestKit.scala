package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  AllNotificationsServer,
  NotificationGrpcServer,
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
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.Config
import org.scalatest.Suite

trait AllNotificationsTestKit
    extends NotificationTestKit[Notification]
    with NotificationGrpcServer[Notification]
    with ApnsToken { _: Suite =>

  implicit lazy val system: ActorSystem[_] = typedSystem()

  lazy val apnsPort: Int = availablePort

  lazy val smsPort: Int = availablePort

  lazy val smtpPort: Int = availablePort

  override lazy val additionalConfig: String = grpcConfig +
    s"""
       |notification.mail.host = $hostname
       |notification.mail.port = $smtpPort
       |notification.push.mock.apns.port = $apnsPort
       |notification.sms.mode.base-url = "http://$interface:$smsPort"
       |""".stripMargin

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new ApnsMockServer with InternalConfig {
        override implicit def system: ActorSystem[_] = typedSystem()

        override def serverPort: Int = apnsPort

        override lazy val config: Config = internalConfig
      }.initMockServer()
    )
    assert(
      new SMSMockServer with InternalConfig {
        override implicit def system: ActorSystem[_] = typedSystem()

        override def serverPort: Int = smsPort

        override def config: Config = internalConfig
      }.initMockServer()
    )
    assert(
      new SmtpMockServer with InternalConfig {
        override implicit def system: ActorSystem[_] = typedSystem()

        override def serverPort: Int = smtpPort

        override lazy val config: Config = internalConfig
      }.initMockServer()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Notification]] =
    _ =>
      Seq(new AllNotificationsBehavior with FcmMockProvider with InternalConfig {
        override def config: Config = internalConfig
      })

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with AllNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${AllNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with AllNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] = system =>
    Seq(AllNotificationsServer(system))
}
