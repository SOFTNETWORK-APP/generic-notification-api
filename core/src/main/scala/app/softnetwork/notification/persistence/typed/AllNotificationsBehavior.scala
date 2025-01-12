package app.softnetwork.notification.persistence.typed

import app.softnetwork.notification.config.{DefaultConfig, InternalConfig}
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.spi.DefaultMailAndSMSAndFcmAndIosAndWsProvider
import org.slf4j.{Logger, LoggerFactory}

trait AllNotificationsBehavior
    extends NotificationBehavior[Notification]
    with DefaultMailAndSMSAndFcmAndIosAndWsProvider { _: InternalConfig =>
  override val persistenceId = "Notification"
}

object AllNotificationsBehavior extends AllNotificationsBehavior with DefaultConfig {
  override def log: Logger = LoggerFactory.getLogger(this.getClass)
}
