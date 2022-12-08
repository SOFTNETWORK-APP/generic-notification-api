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

trait ApnsNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    ApnsNotificationsBehavior.TypeKey
}

trait ApnsNotificationHandler extends NotificationHandler with ApnsNotificationsTypeKey

trait FcmNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    FcmNotificationsBehavior.TypeKey
}

trait FcmNotificationHandler extends NotificationHandler with FcmNotificationsTypeKey

trait FcmAndApnsNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    FcmAndApnsNotificationsBehavior.TypeKey
}

trait FcmAndApnsNotificationHandler extends NotificationHandler with FcmAndApnsNotificationsTypeKey

trait SimpleMailNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    SimpleMailNotificationsBehavior.TypeKey
}

trait SimpleMailNotificationHandler extends NotificationHandler with SimpleMailNotificationsTypeKey

trait SMSModeNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    SMSModeNotificationsBehavior.TypeKey
}

trait SMSModeNotificationHandler extends NotificationHandler with SMSModeNotificationsTypeKey
