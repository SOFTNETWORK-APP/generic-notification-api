package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.spi.{MailProvider, SimpleMailProvider}
import org.softnetwork.notification.model.{Mail, NotificationAck}

trait MailNotificationsBehavior extends NotificationBehavior[Mail] { _: MailProvider =>
  override def persistenceId: String = "MailNotification"

  override def send(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck =
    sendMail(notification)
}

trait SimpleMailNotificationsBehavior extends MailNotificationsBehavior with SimpleMailProvider

object SimpleMailNotificationsBehavior extends SimpleMailNotificationsBehavior
