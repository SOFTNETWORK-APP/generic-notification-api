package app.softnetwork.notification.persistence.typed

import akka.actor.typed.ActorSystem
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
}

trait AndroidNotificationsBehavior extends PushNotificationsBehavior { _: AndroidProvider =>
  override def persistenceId: String = "AndroidNotification"
}

trait FcmNotificationsBehavior extends AndroidNotificationsBehavior with FcmProvider

object FcmNotificationsBehavior extends FcmNotificationsBehavior

trait IosNotificationsBehavior extends PushNotificationsBehavior { _: IosProvider =>
  override def persistenceId: String = "IosNotification"
}

trait ApnsNotificationsBehavior extends IosNotificationsBehavior with ApnsProvider

object ApnsNotificationsBehavior extends ApnsNotificationsBehavior

trait AndroidAndIosNotificationsBehavior extends PushNotificationsBehavior {
  _: AndroidAndIosProvider =>
  override def persistenceId: String = "AndroidAndIosNotification"
}

trait FcmAndApnsNotificationsBehavior
    extends AndroidAndIosNotificationsBehavior
    with FcmAndApnsProvider

object FcmAndApnsNotificationsBehavior extends FcmAndApnsNotificationsBehavior
