package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  FcmAndApnsNotificationsServer,
  NotificationGrpcServer,
  NotificationServer
}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.FcmAndApnsNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  FcmAndApnsNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.notification.spi.{ApnsMockServer, FcmMockAndApnsProvider}
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait FcmAndApnsNotificationsTestKit
    extends NotificationsWithMockServerTestKit[Push]
    with NotificationGrpcServer[Push]
    with ApnsMockServer { _: Suite =>

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(
      new FcmAndApnsNotificationsBehavior with FcmMockAndApnsProvider with InternalConfig {
        override def config: Config = akkaConfig.withFallback(ConfigFactory.load())
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with FcmAndApnsNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String =
            s"${FcmAndApnsNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with FcmAndApnsNotificationsHandler
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
