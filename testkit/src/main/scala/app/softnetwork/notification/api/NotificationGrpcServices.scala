package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import app.softnetwork.api.server.GrpcServices
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification

import scala.concurrent.Future

trait NotificationGrpcServices[T <: Notification] extends GrpcServices {
  _: NotificationGuardian[T] =>

  def interface: String

  def port: Int

  final override def grpcServices
    : ActorSystem[_] => Seq[PartialFunction[HttpRequest, Future[HttpResponse]]] = system =>
    notificationServers(system).map(NotificationServiceApiHandler.partial(_)(system))

  def grpcConfig: String = s"""
                              |# Important: enable HTTP/2 in ActorSystem's config
                              |akka.http.server.preview.enable-http2 = on
                              |akka.grpc.client."${NotificationClient.name}"{
                              |    host = $interface
                              |    port = $port
                              |    use-tls = false
                              |}
                              |""".stripMargin
}
