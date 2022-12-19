package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import app.softnetwork.api.server.scalatest.ServerTestKit
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification
import app.softnetwork.scheduler.api.SchedulerGrpcServices

import scala.concurrent.Future

trait NotificationGrpcServices[T <: Notification] extends SchedulerGrpcServices {
  _: NotificationGuardian[T] with ServerTestKit =>

  override def grpcServices
    : ActorSystem[_] => Seq[PartialFunction[HttpRequest, Future[HttpResponse]]] = system =>
    notificationGrpcServices(system) ++ schedulerGrpcServices(system)

  def notificationGrpcServices
    : ActorSystem[_] => Seq[PartialFunction[HttpRequest, Future[HttpResponse]]] = system =>
    notificationServers(system).map(
      NotificationServiceApiHandler.partial(_)(system)
    )

  def notificationGrpcConfig: String = schedulerGrpcConfig + s"""
                              |# Important: enable HTTP/2 in ActorSystem's config
                              |akka.http.server.preview.enable-http2 = on
                              |akka.grpc.client."${NotificationClient.name}"{
                              |    host = $interface
                              |    port = $port
                              |    use-tls = false
                              |}
                              |""".stripMargin
}
