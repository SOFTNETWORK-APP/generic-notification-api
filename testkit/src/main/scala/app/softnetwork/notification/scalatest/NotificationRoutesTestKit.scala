package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.ApiRoute
import app.softnetwork.notification.launch.{NotificationGuardian, NotificationRoutes}
import app.softnetwork.notification.model.Notification
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}
import app.softnetwork.session.scalatest.{SessionServiceRoutes, SessionTestKit}
import app.softnetwork.session.service.SessionMaterials
import org.scalatest.Suite

trait NotificationRoutesTestKit[SD <: SessionData with SessionDataDecorator[
  SD
], T <: Notification]
    extends NotificationRoutes[SD, T]
    with SessionServiceRoutes[SD] {
  _: Suite with NotificationGuardian[T] with SessionTestKit[SD] with SessionMaterials[SD] =>

  override def apiRoutes: ActorSystem[_] => List[ApiRoute] =
    system =>
      List(
        sessionServiceRoute(system),
        notificationService(system)
      )

}
