package app.softnetwork.notification.handlers

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import app.softnetwork.persistence.typed.scaladsl.EntityPattern
import app.softnetwork.persistence.typed.CommandTypeKey
import app.softnetwork.notification.message._
import app.softnetwork.notification.persistence.typed._

import scala.reflect.ClassTag

/** Created by smanciot on 14/04/2020.
  */

trait AllNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    AllNotificationsBehavior.TypeKey
}

trait NotificationHandler
    extends EntityPattern[NotificationCommand, NotificationCommandResult]
    with AllNotificationsTypeKey
