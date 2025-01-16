package app.softnetwork.notification.api

import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.launch.{NotificationApiRoutes, NotificationApplication}
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.api.SessionDataApi
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait NotificationApi[SD <: SessionData with SessionDataDecorator[SD], T <: Notification]
    extends NotificationApplication[T]
    with SessionDataApi[SD]
    with NotificationApiRoutes[SD, T] { _: ApiRoutes with SchemaProvider => }
