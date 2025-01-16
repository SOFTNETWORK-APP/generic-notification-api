package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.{ApiRoutes, GrpcService}
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerApi
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}

trait NotificationWithSchedulerApi[SD <: SessionData with SessionDataDecorator[
  SD
], T <: Notification]
    extends NotificationApi[SD, T]
    with SchedulerApi {
  _: ApiRoutes with SchemaProvider =>

  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    schedulerEntities(sys) ++ sessionEntities(sys) ++ notificationEntities(sys)

  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    schedulerEventProcessorStreams(sys) ++
    notificationEventProcessorStreams(sys)

  override def grpcServices: ActorSystem[_] => Seq[GrpcService] = system =>
    notificationGrpcServices(system) ++ schedulerGrpcServices(system)
}
