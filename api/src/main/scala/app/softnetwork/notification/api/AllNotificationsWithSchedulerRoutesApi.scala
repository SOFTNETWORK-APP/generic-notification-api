package app.softnetwork.notification.api

import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait AllNotificationsWithSchedulerRoutesApi[SD <: SessionData with SessionDataDecorator[SD]]
    extends AllNotificationsApi[SD]
    with NotificationWithSchedulerRoutesApi[SD, Notification] { _: SchemaProvider => }
