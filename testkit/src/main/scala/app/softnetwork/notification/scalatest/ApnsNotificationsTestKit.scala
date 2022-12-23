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
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.Config
import org.scalatest.Suite
import org.softnetwork.notification.model.Push

trait ApnsNotificationsTestKit
    extends NotificationGrpcServer[Push]
    with NotificationTestKit[Push]
    with ApnsToken { _: Suite =>

  lazy val apnsPort: Int = availablePort

  override lazy val additionalConfig: String = notificationGrpcConfig +
    s"""
      |notification.push.mock.apns.port = $apnsPort
      |""".stripMargin

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new ApnsMockServer with InternalConfig {
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = apnsPort

        override lazy val config: Config = internalConfig
      }.init()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(new ApnsNotificationsBehavior with InternalConfig {
      override def config: Config = internalConfig
    })

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with ApnsNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${ApnsNotificationsBehavior.persistenceId}-scheduler"
          override val forTests: Boolean = true
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
