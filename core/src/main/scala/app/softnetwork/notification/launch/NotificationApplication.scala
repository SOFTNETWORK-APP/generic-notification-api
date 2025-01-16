package app.softnetwork.notification.launch

import app.softnetwork.api.server.launch.HealthCheckApplication
import app.softnetwork.notification.api.NotificationGrpcServices
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.CsrfCheck

trait NotificationApplication[T <: Notification]
    extends HealthCheckApplication
    with NotificationGuardian[T]
    with NotificationGrpcServices[T] { _: SchemaProvider with CsrfCheck => }
