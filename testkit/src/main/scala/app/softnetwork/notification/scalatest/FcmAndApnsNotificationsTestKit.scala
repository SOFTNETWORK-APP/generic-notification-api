package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  FcmAndApnsNotificationsServer,
  NotificationGrpcServerTestKit,
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
import app.softnetwork.notification.spi.FcmMockAndApnsProvider
import app.softnetwork.persistence.query.{InMemoryJournalProvider, InMemoryOffsetProvider}
import app.softnetwork.scheduler.config.SchedulerSettings
import com.typesafe.config.Config
import org.scalatest.Suite
import app.softnetwork.notification.model.Push
import org.slf4j.{Logger, LoggerFactory}

trait FcmAndApnsNotificationsTestKit
    extends NotificationGrpcServerTestKit[Push]
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
        lazy val log: Logger = LoggerFactory getLogger getClass.getName
        override implicit def system: ActorSystem[_] = asystem

        override def serverPort: Int = apnsPort

        override lazy val config: Config = internalConfig
      }.init()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Push]] = _ =>
    Seq(
      new FcmAndApnsNotificationsBehavior with FcmMockAndApnsProvider with InternalConfig {
        override def config: Config = internalConfig
        override def log: Logger = LoggerFactory.getLogger(this.getClass)
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with FcmAndApnsNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val tag: String =
            SchedulerSettings.tag(FcmAndApnsNotificationsBehavior.persistenceId)
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with FcmAndApnsNotificationsHandler
          with InMemoryJournalProvider
          with InMemoryOffsetProvider {
          lazy val log: Logger = LoggerFactory getLogger getClass.getName
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(FcmAndApnsNotificationsServer(system))
}
