package app.softnetwork.notification.api

import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerEndpointsApi
import app.softnetwork.session.CsrfCheck
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait AllNotificationsWithSchedulerEndpointsApi[SD <: SessionData with SessionDataDecorator[SD]]
    extends AllNotificationsApi[SD]
    with NotificationWithSchedulerApi[SD, Notification]
    with SchedulerEndpointsApi { _: SchemaProvider with CsrfCheck => }
