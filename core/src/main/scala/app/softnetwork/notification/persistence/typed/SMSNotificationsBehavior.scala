package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.spi.{SMSModeProvider, SMSProvider}
import org.softnetwork.notification.model.{NotificationAck, SMS}

trait SMSNotificationsBehavior extends NotificationBehavior[SMS] { _: SMSProvider =>
  override def persistenceId: String = "SMSNotification"

  override def send(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck = sendSMS(
    notification
  )
}

trait SMSModeNotificationsBehavior extends SMSNotificationsBehavior with SMSModeProvider

object SMSModeNotificationsBehavior extends SMSModeNotificationsBehavior
