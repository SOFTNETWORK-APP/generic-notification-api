package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  FcmNotificationsServer,
  NotificationGrpcServer,
  NotificationServer
}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.FcmNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  FcmNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.notification.spi.FcmMockProvider
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait FcmNotificationsTestKit extends NotificationTestKit[Push] with NotificationGrpcServer[Push] {
  _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(
      new FcmNotificationsBehavior with FcmMockProvider with InternalConfig {
        override def config: Config = internalConfig
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with FcmNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${FcmNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with FcmNotificationsHandler
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
