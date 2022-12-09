package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{FcmAndApnsNotificationsServer, NotificationServer}
import app.softnetwork.notification.handlers.FcmMockAndApnsNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  FcmMockAndApnsNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.notification.spi.ApnsMockServer
import app.softnetwork.persistence.query.InMemoryJournalProvider
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait FcmAndApnsNotificationsTestKit
    extends NotificationsWithMockServerTestKit[Push]
    with ApnsMockServer { _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(FcmMockAndApnsNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with FcmMockAndApnsNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String =
            s"${FcmMockAndApnsNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with FcmMockAndApnsNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(FcmAndApnsNotificationsServer(system))
}
