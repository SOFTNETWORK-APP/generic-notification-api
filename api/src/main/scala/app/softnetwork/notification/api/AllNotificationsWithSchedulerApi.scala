package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.GrpcService
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerApi

trait AllNotificationsWithSchedulerApi extends AllNotificationsApi with SchedulerApi {
  _: SchemaProvider =>

  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    schedulerEntities(sys) ++ notificationEntities(sys)

  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    schedulerEventProcessorStreams(sys) ++
    notificationEventProcessorStreams(sys)

  override def grpcServices: ActorSystem[_] => Seq[GrpcService] = system =>
    notificationGrpcServices(system) ++ schedulerGrpcServices(system)
}
