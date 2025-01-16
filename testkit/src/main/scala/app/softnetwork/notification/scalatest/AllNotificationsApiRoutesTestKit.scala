package app.softnetwork.notification.scalatest

import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.model.Notification
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}
import app.softnetwork.session.service.SessionMaterials
import org.scalatest.Suite

trait AllNotificationsApiRoutesTestKit[SD <: SessionData with SessionDataDecorator[SD]]
    extends NotificationApiRoutesTestKit[SD, Notification]
    with AllNotificationsTestKit { _: Suite with ApiRoutes with SessionMaterials[SD] => }
