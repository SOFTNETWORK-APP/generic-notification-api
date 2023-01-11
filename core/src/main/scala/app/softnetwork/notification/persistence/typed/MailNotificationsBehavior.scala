package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.spi.{MailProvider, SimpleMailProvider}
import app.softnetwork.notification.model.{Mail, NotificationAck}

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

object SimpleMailNotificationsBehavior extends SimpleMailNotificationsBehavior with DefaultConfig
