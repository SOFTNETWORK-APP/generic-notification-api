package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.{SchedulerApi, SchedulerServiceApiHandler}

import scala.concurrent.Future

trait AllNotificationsWithSchedulerApi extends AllNotificationsApi with SchedulerApi {
  _: SchemaProvider =>

  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    schedulerEntities(sys) ++ notificationEntities(sys)

  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    schedulerEventProcessorStreams(sys) ++
    notificationEventProcessorStreams(sys)

  override def grpcServices
    : ActorSystem[_] => Seq[PartialFunction[HttpRequest, Future[HttpResponse]]] = system =>
    notificationServers(system).map(
      NotificationServiceApiHandler.partial(_)(system)
    ) :+ SchedulerServiceApiHandler.partial(schedulerServer(system))(system)
}
