package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.ApiRoute
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerRoutesApi
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait NotificationWithSchedulerRoutesApi[SD <: SessionData with SessionDataDecorator[
  SD
], T <: Notification]
    extends NotificationWithSchedulerApi[SD, T]
    with NotificationRoutesApi[SD, T]
    with SchedulerRoutesApi {
  _: NotificationApi[SD, T] with SchemaProvider =>

  override def apiRoutes: ActorSystem[_] => List[ApiRoute] =
    system =>
      List(
        notificationService(system),
        schedulerService(system)
      )

}
