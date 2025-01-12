package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import akka.grpc.GrpcClientSettings
import app.softnetwork.api.server.client.{GrpcClient, GrpcClientFactory}
import app.softnetwork.notification.model.{Mail, NotificationStatusResult, Push, SMS, Ws}

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

  def addWs(ws: Ws): Future[Boolean] = {
    grpcClient.addWs(AddWsRequest(Some(ws))) map (_.succeeded)
  }

  def removeNotification(uuid: String): Future[Boolean] = {
    grpcClient.removeNotification(RemoveNotificationRequest(uuid)) map (_.succeeded)
  }

  def sendMail(mail: Mail): Future[Seq[NotificationStatusResult]] = {
    grpcClient.sendMail(SendMailRequest(Some(mail))) map (_.results)
  }

  def sendSMS(sms: SMS): Future[Seq[NotificationStatusResult]] = {
    grpcClient.sendSMS(SendSMSRequest(Some(sms))) map (_.results)
  }

  def sendPush(push: Push): Future[Seq[NotificationStatusResult]] = {
    grpcClient.sendPush(SendPushRequest(Some(push))) map (_.results)
  }

  def sendWs(ws: Ws): Future[Seq[NotificationStatusResult]] = {
    grpcClient.sendWs(SendWsRequest(Some(ws))) map (_.results)
  }

  def getNotificationStatus(uuid: String): Future[Seq[NotificationStatusResult]] = {
    grpcClient.getNotificationStatus(GetNotificationStatusRequest(uuid)) map (_.results)
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
