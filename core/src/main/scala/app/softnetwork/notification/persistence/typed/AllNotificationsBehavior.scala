package app.softnetwork.notification.persistence.typed

import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.spi.DefaultMailAndSMSAndFcmAndIosProvider

trait AllNotificationsBehavior
    extends NotificationBehavior[Notification]
    with DefaultMailAndSMSAndFcmAndIosProvider {
  override val persistenceId = "Notification"
}

object AllNotificationsBehavior extends AllNotificationsBehavior
