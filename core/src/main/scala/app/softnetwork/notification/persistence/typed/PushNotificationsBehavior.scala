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
import app.softnetwork.notification.model.{NotificationAck, Push}
import org.slf4j.{Logger, LoggerFactory}

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

object FcmNotificationsBehavior extends FcmNotificationsBehavior with DefaultConfig {
  override def log: Logger = LoggerFactory.getLogger(this.getClass)
}

trait IosNotificationsBehavior extends PushNotificationsBehavior { _: IosProvider =>
  override def persistenceId: String = "IosNotification"
}

trait ApnsNotificationsBehavior extends IosNotificationsBehavior with ApnsProvider {
  _: InternalConfig =>
}

object ApnsNotificationsBehavior extends ApnsNotificationsBehavior with DefaultConfig {
  override def log: Logger = LoggerFactory.getLogger(this.getClass)
}

trait AndroidAndIosNotificationsBehavior extends PushNotificationsBehavior {
  _: AndroidAndIosProvider =>
  override def persistenceId: String = "AndroidAndIosNotification"
}

trait FcmAndApnsNotificationsBehavior
    extends AndroidAndIosNotificationsBehavior
    with FcmAndApnsProvider { _: InternalConfig => }

object FcmAndApnsNotificationsBehavior extends FcmAndApnsNotificationsBehavior with DefaultConfig {
  override def log: Logger = LoggerFactory.getLogger(this.getClass)
}
