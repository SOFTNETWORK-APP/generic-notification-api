package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.{GrpcService, GrpcServices}
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification

trait NotificationGrpcServices[T <: Notification] extends GrpcServices {
  _: NotificationGuardian[T] =>
  override def grpcServices: ActorSystem[_] => Seq[GrpcService] = system =>
    notificationGrpcServices(system)
}
