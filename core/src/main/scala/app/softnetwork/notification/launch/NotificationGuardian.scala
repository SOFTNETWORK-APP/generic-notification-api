package app.softnetwork.notification.launch

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.GrpcService
import app.softnetwork.notification.NotificationCoreBuildInfo
import app.softnetwork.notification.api.{NotificationGrpcService, NotificationServer}
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{NotificationBehavior, WsClientsBehavior}
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.CsrfCheckHeader
import app.softnetwork.session.launch.SessionGuardian

import scala.language.implicitConversions

trait NotificationGuardian[T <: Notification] extends SessionGuardian with CsrfCheckHeader {
  _: SchemaProvider =>

  import app.softnetwork.persistence.launch.PersistenceGuardian._

  def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[T]] = _ => Seq.empty

  def notificationEntities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    notificationBehaviors(sys).map(entity2PersistentEntity) :+ entity2PersistentEntity(
      WsClientsBehavior
    )

  /** initialize all entities
    */
  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    sessionEntities(sys) ++ notificationEntities(sys)

  def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] = _ => None

  def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] = _ => None

  def notificationEventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    Seq(scheduler2NotificationProcessorStream(sys)).flatten ++
    Seq(notificationCommandProcessorStream(sys)).flatten

  /** initialize all event processor streams
    */
  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    notificationEventProcessorStreams(sys)

  /** initialize all notification servers
    */
  def notificationServers: ActorSystem[_] => Seq[NotificationServer]

  def notificationGrpcServices: ActorSystem[_] => Seq[GrpcService] = sys =>
    notificationServers(sys).map(new NotificationGrpcService(_))

  override def systemVersion(): String =
    sys.env.getOrElse("VERSION", NotificationCoreBuildInfo.version)
}
