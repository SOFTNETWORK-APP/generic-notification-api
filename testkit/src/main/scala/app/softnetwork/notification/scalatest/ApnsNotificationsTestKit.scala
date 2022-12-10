package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  ApnsNotificationsServer,
  NotificationGrpcServer,
  NotificationServer
}
import app.softnetwork.notification.handlers.ApnsNotificationsHandler
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
    with NotificationGrpcServer[Push]
    with ApnsMockServer { _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(ApnsNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with ApnsNotificationsHandler
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
          with ApnsNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(ApnsNotificationsServer(system))

}
