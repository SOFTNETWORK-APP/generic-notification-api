package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.spi.{
  AndroidAndIosProvider,
  AndroidProvider,
  ApnsProvider,
  FcmAndApnsProvider,
  FcmProvider,
  IosProvider,
  PushProvider
}
import org.softnetwork.notification.model.{NotificationAck, Push}

trait PushNotificationsBehavior extends NotificationBehavior[Push] { _: PushProvider =>
  override def send(notification: Push)(implicit system: ActorSystem[_]): NotificationAck =
    sendPush(notification)

  override def ack(notification: Push)(implicit system: ActorSystem[_]): NotificationAck = ackPush(
    notification
  )
}

trait AndroidNotificationsBehavior extends PushNotificationsBehavior { _: AndroidProvider =>
  override def persistenceId: String = "AndroidNotification"
}

trait FcmNotificationsBehavior extends AndroidNotificationsBehavior with FcmProvider {
  _: InternalConfig =>
}

object FcmNotificationsBehavior extends FcmNotificationsBehavior with DefaultConfig

trait IosNotificationsBehavior extends PushNotificationsBehavior { _: IosProvider =>
  override def persistenceId: String = "IosNotification"
}

trait ApnsNotificationsBehavior extends IosNotificationsBehavior with ApnsProvider {
  _: InternalConfig =>
}

object ApnsNotificationsBehavior extends ApnsNotificationsBehavior with DefaultConfig

trait AndroidAndIosNotificationsBehavior extends PushNotificationsBehavior {
  _: AndroidAndIosProvider =>
  override def persistenceId: String = "AndroidAndIosNotification"
}

trait FcmAndApnsNotificationsBehavior
    extends AndroidAndIosNotificationsBehavior
    with FcmAndApnsProvider { _: InternalConfig => }

object FcmAndApnsNotificationsBehavior extends FcmAndApnsNotificationsBehavior with DefaultConfig
