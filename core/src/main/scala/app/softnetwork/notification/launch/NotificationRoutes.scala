package app.softnetwork.notification.launch

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.{ApiRoute, ApiRoutes}
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.serialization.notificationFormats
import app.softnetwork.notification.service.NotificationService
import app.softnetwork.persistence.schema.SchemaProvider
import org.json4s.Formats

trait NotificationRoutes[T <: Notification] extends ApiRoutes {
  _: NotificationGuardian[T] with SchemaProvider =>

  override implicit def formats: Formats = notificationFormats

  def notificationService: ActorSystem[_] => NotificationService = sys =>
    new NotificationService {
      override implicit def system: ActorSystem[_] = sys
    }

  override def apiRoutes: ActorSystem[_] => List[ApiRoute] =
    system =>
      List(
        notificationService(system)
      )

}
