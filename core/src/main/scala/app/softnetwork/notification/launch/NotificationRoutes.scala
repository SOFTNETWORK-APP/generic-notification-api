package app.softnetwork.notification.launch

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.{ApiRoute, ApiRoutes}
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.serialization.notificationFormats
import app.softnetwork.notification.service.NotificationService
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}
import org.json4s.Formats

trait NotificationRoutes[SD <: SessionData with SessionDataDecorator[SD], T <: Notification]
    extends NotificationApiRoutes[SD, T]
    with ApiRoutes {
  _: NotificationGuardian[T] with SchemaProvider =>

  override implicit def formats: Formats = notificationFormats

  def notificationService: ActorSystem[_] => NotificationService[SD]

  override def apiRoutes: ActorSystem[_] => List[ApiRoute] =
    system =>
      List(
        notificationService(system)
      )

}
