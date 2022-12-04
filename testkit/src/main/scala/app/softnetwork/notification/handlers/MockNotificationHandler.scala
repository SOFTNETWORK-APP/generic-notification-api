package app.softnetwork.notification.handlers

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import app.softnetwork.notification.message.NotificationCommand
import app.softnetwork.notification.persistence.typed.MockAllNotificationsBehavior
import app.softnetwork.persistence.typed.CommandTypeKey

import scala.reflect.ClassTag

trait MockAllNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    MockAllNotificationsBehavior.TypeKey
}

trait MockNotificationHandler extends NotificationHandler with MockAllNotificationsTypeKey
