package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.{
  NotificationGrpcServer,
  NotificationServer,
  SimpleMailNotificationsServer
}
import app.softnetwork.notification.config.InternalConfig
import app.softnetwork.notification.handlers.SimpleMailNotificationsHandler
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  NotificationBehavior,
  SimpleMailNotificationsBehavior
}
import app.softnetwork.notification.spi.SmtpMockServer
import app.softnetwork.persistence.query.InMemoryJournalProvider
import com.typesafe.config.Config
import org.scalatest.Suite
import org.softnetwork.notification.model.Mail

import java.net.ServerSocket

trait SimpleMailNotificationsTestKit
    extends NotificationTestKit[Mail]
    with NotificationGrpcServer[Mail] { _: Suite =>

  implicit lazy val system: ActorSystem[Nothing] = typedSystem()

  override lazy val additionalConfig: String = grpcConfig +
    s"""
       |notification.mail.host = $hostname
       |notification.mail.port = ${new ServerSocket(0).getLocalPort}
       |""".stripMargin

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(
      new SmtpMockServer with InternalConfig {
        override implicit def system: ActorSystem[_] = typedSystem()

        override lazy val config: Config = internalConfig
      }.start()
    )
  }

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Mail]] = _ =>
    Seq(
      new SimpleMailNotificationsBehavior with InternalConfig {
        override lazy val config: Config = internalConfig
      }
    )

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream
          with SimpleMailNotificationsHandler
          with InMemoryJournalProvider {
          override val tag: String = s"${SimpleMailNotificationsBehavior.persistenceId}-scheduler"
          override protected val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with SimpleMailNotificationsHandler
          with InMemoryJournalProvider {
          override val forTests: Boolean = true
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] =
    system => Seq(SimpleMailNotificationsServer(system))

}
