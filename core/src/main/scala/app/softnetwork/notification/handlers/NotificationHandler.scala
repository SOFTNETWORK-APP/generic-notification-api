package app.softnetwork.notification.handlers

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import app.softnetwork.persistence.typed.scaladsl.EntityPattern
import app.softnetwork.persistence.typed.CommandTypeKey
import app.softnetwork.notification.message._
import app.softnetwork.notification.persistence.typed._

import scala.reflect.ClassTag

/** Created by smanciot on 14/04/2020.
  */

trait NotificationHandler extends EntityPattern[NotificationCommand, NotificationCommandResult] {
  _: CommandTypeKey[NotificationCommand] =>
}

trait AllNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    AllNotificationsBehavior.TypeKey
}

trait AllNotificationsHandler extends NotificationHandler with AllNotificationsTypeKey

trait ApnsNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    ApnsNotificationsBehavior.TypeKey
}

trait ApnsNotificationsHandler extends NotificationHandler with ApnsNotificationsTypeKey

trait FcmNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    FcmNotificationsBehavior.TypeKey
}

trait FcmNotificationsHandler extends NotificationHandler with FcmNotificationsTypeKey

trait FcmAndApnsNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    FcmAndApnsNotificationsBehavior.TypeKey
}

trait FcmAndApnsNotificationsHandler extends NotificationHandler with FcmAndApnsNotificationsTypeKey

trait SimpleMailNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    SimpleMailNotificationsBehavior.TypeKey
}

trait SimpleMailNotificationsHandler extends NotificationHandler with SimpleMailNotificationsTypeKey

trait SMSModeNotificationsTypeKey extends CommandTypeKey[NotificationCommand] {
  override def TypeKey(implicit
    tTag: ClassTag[NotificationCommand]
  ): EntityTypeKey[NotificationCommand] =
    SMSModeNotificationsBehavior.TypeKey
}

trait SMSModeNotificationsHandler extends NotificationHandler with SMSModeNotificationsTypeKey
