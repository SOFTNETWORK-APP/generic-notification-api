package app.softnetwork.notification.handlers

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import app.softnetwork.notification.message.NotificationCommand
import app.softnetwork.notification.persistence.typed.FcmMockNotificationsBehavior
import app.softnetwork.persistence.typed.CommandTypeKey

import scala.reflect.ClassTag

trait FcmMockNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    FcmMockNotificationsBehavior.TypeKey
}

trait FcmMockNotificationsHandler extends NotificationHandler with FcmMockNotificationsTypeKey
