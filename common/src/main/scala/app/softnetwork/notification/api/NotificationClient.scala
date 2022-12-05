package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.grpc.GrpcClientSettings
import org.softnetwork.notification.model.{Mail, Push, SMS}

import scala.concurrent.Future

trait NotificationClient extends GrpcClient {

  implicit lazy val grpcClient: NotificationServiceApiClient =
    NotificationServiceApiClient(
      GrpcClientSettings.fromConfig(name)
    )

  def addMail(mail: Mail): Future[Boolean] = {
    grpcClient.addMail(AddMailRequest(Some(mail))) map (_.succeeded)
  }

  def addSMS(sms: SMS): Future[Boolean] = {
    grpcClient.addSMS(AddSMSRequest(Some(sms))) map (_.succeeded)
  }

  def addPush(push: Push): Future[Boolean] = {
    grpcClient.addPush(AddPushRequest(Some(push))) map (_.succeeded)
  }
}

object NotificationClient extends GrpcClientFactory[NotificationClient] {
  override val name: String = "NotificationService"
  override def init(sys: ActorSystem[_]): NotificationClient = {
    new NotificationClient {
      override implicit lazy val system: ActorSystem[_] = sys
      val name: String = NotificationClient.name
    }
  }
}
