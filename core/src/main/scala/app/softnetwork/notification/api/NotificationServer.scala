package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.NotificationHandler
import app.softnetwork.notification.message.{
  AddNotification,
  GetNotificationStatus,
  NotificationAdded,
  NotificationRemoved,
  NotificationResults,
  RemoveNotification,
  SendNotification
}
import app.softnetwork.notification.model.Notification

import scala.concurrent.{ExecutionContextExecutor, Future}

trait NotificationServer extends NotificationServiceApi {
  _: NotificationHandler =>
  implicit def system: ActorSystem[_]

  implicit lazy val ec: ExecutionContextExecutor = system.executionContext

  override def addMail(in: AddMailRequest): Future[AddNotificationResponse] = {
    addNotification(in.mail)
  }

  override def addSMS(in: AddSMSRequest): Future[AddNotificationResponse] = {
    addNotification(in.sms)
  }

  override def addPush(in: AddPushRequest): Future[AddNotificationResponse] = {
    addNotification(in.push)
  }

  override def addWs(in: AddWsRequest): Future[AddNotificationResponse] = {
    addNotification(in.ws)
  }

  private def addNotification(maybe: Option[Notification]): Future[AddNotificationResponse] = {
    maybe match {
      case Some(notification) =>
        ?(notification.uuid, AddNotification(notification)) map {
          case _: NotificationAdded => AddNotificationResponse(true)
          case _                    => AddNotificationResponse()
        }
      case _ => Future.successful(AddNotificationResponse())
    }
  }

  override def removeNotification(
    in: RemoveNotificationRequest
  ): Future[RemoveNotificationResponse] = {
    ?(in.uuid, RemoveNotification(in.uuid)) map {
      case NotificationRemoved => RemoveNotificationResponse(true)
      case _                   => RemoveNotificationResponse()
    }
  }

  override def sendMail(in: SendMailRequest): Future[SendNotificationResponse] = {
    sendNotification(in.mail)
  }

  override def sendSMS(in: SendSMSRequest): Future[SendNotificationResponse] = {
    sendNotification(in.sms)
  }

  override def sendPush(in: SendPushRequest): Future[SendNotificationResponse] = {
    sendNotification(in.push)
  }

  override def sendWs(in: SendWsRequest): Future[SendNotificationResponse] = {
    sendNotification(in.ws)
  }

  protected def sendNotification(maybe: Option[Notification]): Future[SendNotificationResponse] = {
    maybe match {
      case Some(notification) =>
        ?(notification.uuid, SendNotification(notification)) map {
          case r: NotificationResults => SendNotificationResponse(r.results)
          case _                      => SendNotificationResponse()
        }
      case _ => Future.successful(SendNotificationResponse())
    }
  }

  override def getNotificationStatus(
    in: GetNotificationStatusRequest
  ): Future[GetNotificationStatusResponse] = {
    ?(in.uuid, GetNotificationStatus(in.uuid)) map {
      case r: NotificationResults => GetNotificationStatusResponse(r.results)
      case _                      => GetNotificationStatusResponse()
    }
  }
}
