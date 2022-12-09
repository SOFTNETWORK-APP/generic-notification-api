package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{MockAllNotificationsServer, NotificationServer}
import app.softnetwork.notification.handlers.MockAllNotificationsHandler
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  MockAllNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.persistence.query.InMemoryJournalProvider
import org.scalatest.Suite

trait AllNotificationsTestKit extends NotificationTestKit[Notification] {
  _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Notification]] =
    _ => Seq(MockAllNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with MockAllNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${MockAllNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with MockAllNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] = system =>
    Seq(MockAllNotificationsServer(system))
}
