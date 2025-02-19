package app.softnetwork.notification.scalatest

import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.model.Notification
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}
import app.softnetwork.session.scalatest.SessionTestKit
import app.softnetwork.session.service.SessionMaterials
import org.scalatest.Suite

trait NotificationApiRoutesTestKit[SD <: SessionData with SessionDataDecorator[
  SD
], T <: Notification]
    extends SessionTestKit[SD]
    with NotificationTestKit[T] { _: Suite with ApiRoutes with SessionMaterials[SD] => }
