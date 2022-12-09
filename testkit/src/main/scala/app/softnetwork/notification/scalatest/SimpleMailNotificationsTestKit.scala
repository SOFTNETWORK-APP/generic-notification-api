package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  FcmNotificationsServer,
  NotificationServer,
  SimpleMailNotificationsServer
}
import app.softnetwork.notification.handlers.{
  FcmMockNotificationsHandler,
  SimpleMailNotificationsHandler
}
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  FcmMockNotificationsBehavior,
  NotificationBehavior,
  SimpleMailNotificationsBehavior
}
import app.softnetwork.notification.spi.SmtpMockServer
import app.softnetwork.persistence.query.InMemoryJournalProvider
import org.scalatest.Suite
import org.softnetwork.notification.model.{Mail, Push}

trait SimpleMailNotificationsTestKit extends NotificationTestKit[Mail] { _: Suite =>

  implicit lazy val system: ActorSystem[Nothing] = typedSystem()

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(SmtpMockServer(system).start())
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Mail]] = _ =>
    Seq(SimpleMailNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with SimpleMailNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${SimpleMailNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with SimpleMailNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(SimpleMailNotificationsServer(system))

}
