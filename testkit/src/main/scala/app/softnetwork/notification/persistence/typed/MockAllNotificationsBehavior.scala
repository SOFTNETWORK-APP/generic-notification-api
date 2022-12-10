package app.softnetwork.notification.persistence.typed

import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.spi.MockNotificationProvider

trait MockAllNotificationsBehavior extends AllNotificationsBehavior with MockNotificationProvider {
  _: InternalConfig =>
  override val persistenceId = "MockNotification"
}

object MockAllNotificationsBehavior extends MockAllNotificationsBehavior with DefaultConfig
