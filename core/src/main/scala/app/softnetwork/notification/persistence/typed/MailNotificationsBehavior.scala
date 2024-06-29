package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.spi.{MailProvider, SimpleMailProvider}
import app.softnetwork.notification.model.{Mail, NotificationAck}
import org.slf4j.{Logger, LoggerFactory}

trait MailNotificationsBehavior extends NotificationBehavior[Mail] { _: MailProvider =>
  override def persistenceId: String = "MailNotification"

  override def send(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck =
    sendMail(notification)

  override def ack(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck = ackMail(
    notification
  )
}

trait SimpleMailNotificationsBehavior extends MailNotificationsBehavior with SimpleMailProvider {
  _: InternalConfig =>
}

object SimpleMailNotificationsBehavior extends SimpleMailNotificationsBehavior with DefaultConfig {
  override def log: Logger = LoggerFactory.getLogger(this.getClass)
}
