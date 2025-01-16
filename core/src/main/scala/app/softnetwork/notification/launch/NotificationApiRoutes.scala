package app.softnetwork.notification.launch

import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait NotificationApiRoutes[SD <: SessionData with SessionDataDecorator[SD], T <: Notification] {
  _: NotificationGuardian[T] with ApiRoutes with SchemaProvider =>
}
