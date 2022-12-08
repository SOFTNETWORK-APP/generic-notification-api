package app.softnetwork.notification.launch

import app.softnetwork.api.server.launch.HealthCheckApplication
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.query.SchemaProvider

trait NotificationApplication[T <: Notification]
    extends HealthCheckApplication
    with NotificationGuardian[T] {
  _: SchemaProvider =>
}
