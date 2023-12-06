package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.GrpcService
import app.softnetwork.api.server.scalatest.ServerTestKit
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification
import app.softnetwork.scheduler.api.SchedulerGrpcServicesTestKit
import app.softnetwork.scheduler.launch.SchedulerGuardian

trait NotificationGrpcServicesTestKit[T <: Notification] extends SchedulerGrpcServicesTestKit {
  _: NotificationGuardian[T] with SchedulerGuardian with ServerTestKit =>

  override def grpcServices: ActorSystem[_] => Seq[GrpcService] = system =>
    notificationGrpcServices(system) ++ schedulerGrpcServices(system)

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
