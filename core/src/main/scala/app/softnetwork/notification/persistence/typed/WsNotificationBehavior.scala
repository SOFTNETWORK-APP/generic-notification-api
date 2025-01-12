package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.DefaultConfig
import app.softnetwork.notification.model.{NotificationAck, Ws}
import app.softnetwork.notification.spi.WsProvider

trait WsNotificationBehavior extends NotificationBehavior[Ws] with WsProvider {

  override def persistenceId: String = "WsNotification"

  override def send(
    notification: Ws
  )(implicit system: ActorSystem[_]): NotificationAck = sendWs(notification)

  override def ack(notification: Ws)(implicit system: ActorSystem[_]): NotificationAck = ackClient(
    notification
  )
}

object WsNotificationBehavior extends WsNotificationBehavior with DefaultConfig
