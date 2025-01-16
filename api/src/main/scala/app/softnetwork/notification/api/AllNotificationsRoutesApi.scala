package app.softnetwork.notification.api

import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait AllNotificationsRoutesApi[SD <: SessionData with SessionDataDecorator[SD]]
    extends AllNotificationsApi[SD]
    with NotificationRoutesApi[SD, Notification] { _: SchemaProvider => }
