package app.softnetwork.notification.persistence.typed

import app.softnetwork.notification.spi.MockNotificationProvider

trait MockAllNotificationsBehavior extends AllNotificationsBehavior with MockNotificationProvider {
  override val persistenceId = "MockNotification"
}

object MockAllNotificationsBehavior extends MockAllNotificationsBehavior
