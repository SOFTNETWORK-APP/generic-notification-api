package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import app.softnetwork.api.server.GrpcService

import scala.concurrent.Future

class NotificationGrpcService(server: NotificationServer) extends GrpcService {
  override def grpcService: ActorSystem[_] => PartialFunction[HttpRequest, Future[HttpResponse]] =
    system => NotificationServiceApiHandler.partial(server)(system)
}
