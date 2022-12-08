package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.ApnsNotificationHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  ApnsNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.notification.spi.ApnsMockServer
import app.softnetwork.persistence.query.InMemoryJournalProvider
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait ApnsNotificationsTestKit
    extends NotificationsWithMockServerTestKit[Push]
    with ApnsMockServer { _: Suite =>

  override def notificationBehavior: ActorSystem[_] => Option[NotificationBehavior[Push]] = _ =>
    Some(ApnsNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with ApnsNotificationHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${ApnsNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with ApnsNotificationHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )
}
