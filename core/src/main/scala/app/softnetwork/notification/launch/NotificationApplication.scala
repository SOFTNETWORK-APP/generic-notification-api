package app.softnetwork.notification.launch

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import app.softnetwork.api.server.launch.HealthCheckApplication
import app.softnetwork.notification.api.NotificationServiceApiHandler
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.query.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerServiceApiHandler

import scala.concurrent.Future

trait NotificationApplication[T <: Notification]
    extends HealthCheckApplication
    with NotificationGuardian[T] {
  _: SchemaProvider =>
  override def grpcServices
    : ActorSystem[_] => Seq[PartialFunction[HttpRequest, Future[HttpResponse]]] = system =>
    notificationServers(system).map(
      NotificationServiceApiHandler.partial(_)(system)
    ) :+ SchedulerServiceApiHandler.partial(schedulerServer(system))(system)
}
