package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  ApnsNotificationsServer,
  NotificationGrpcServer,
  NotificationServer
}
import app.softnetwork.notification.config.InternalConfig
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
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

import java.net.ServerSocket

trait ApnsNotificationsTestKit
    extends NotificationsWithMockServerTestKit[Push]
    with NotificationGrpcServer[Push]
    with ApnsMockServer
    with InternalConfig { _: Suite =>

  override lazy val additionalConfig: String = grpcConfig +
    s"""
      |notification.push.mock.apns.port = ${new ServerSocket(0).getLocalPort}
      |""".stripMargin

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(new ApnsNotificationsBehavior with InternalConfig {
      override def config: Config = akkaConfig.withFallback(ConfigFactory.load())
    })

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
