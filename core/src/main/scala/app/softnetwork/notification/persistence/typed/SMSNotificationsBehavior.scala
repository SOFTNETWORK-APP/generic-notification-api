package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.spi.{SMSModeProvider, SMSProvider}
import app.softnetwork.notification.model.{NotificationAck, SMS}

trait SMSNotificationsBehavior extends NotificationBehavior[SMS] { _: SMSProvider =>
  override def persistenceId: String = "SMSNotification"

  override def send(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck = sendSMS(
    notification
  )

  override def ack(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck = ackSMS(
    notification
  )
}

trait SMSModeNotificationsBehavior extends SMSNotificationsBehavior with SMSModeProvider {
  _: InternalConfig =>
}

object SMSModeNotificationsBehavior extends SMSModeNotificationsBehavior with DefaultConfig
