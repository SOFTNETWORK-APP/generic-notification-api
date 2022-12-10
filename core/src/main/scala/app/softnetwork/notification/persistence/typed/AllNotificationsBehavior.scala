package app.softnetwork.notification.persistence.typed

import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.spi.DefaultMailAndSMSAndFcmAndIosProvider

trait AllNotificationsBehavior
    extends NotificationBehavior[Notification]
    with DefaultMailAndSMSAndFcmAndIosProvider { _: InternalConfig =>
  override val persistenceId = "Notification"
}

object AllNotificationsBehavior extends AllNotificationsBehavior with DefaultConfig
