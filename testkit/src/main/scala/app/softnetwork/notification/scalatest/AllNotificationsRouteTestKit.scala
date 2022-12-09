package app.softnetwork.notification.scalatest

import app.softnetwork.notification.model.Notification
import org.scalatest.Suite

trait AllNotificationsRouteTestKit extends NotificationRouteTestKit[Notification] {
  _: Suite =>
}
