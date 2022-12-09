package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{FcmNotificationsServer, NotificationServer}
import app.softnetwork.notification.handlers.FcmMockNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  FcmMockNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.persistence.query.InMemoryJournalProvider
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait FcmNotificationsTestKit extends NotificationTestKit[Push] { _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(FcmMockNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with FcmMockNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${FcmMockNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with FcmMockNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(FcmNotificationsServer(system))

}
