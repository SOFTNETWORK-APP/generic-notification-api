package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.NotificationHandler
import app.softnetwork.notification.message.{AddNotification, NotificationAdded}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait NotificationServer extends NotificationServiceApi {
  _: NotificationHandler =>
  implicit def system: ActorSystem[_]

  implicit lazy val ec: ExecutionContextExecutor = system.executionContext

  override def addMail(in: AddMailRequest): Future[AddNotificationResponse] = {
    in.mail match {
      case Some(mail) =>
        ?(mail.uuid, AddNotification(mail)) map {
          case _: NotificationAdded => AddNotificationResponse(true)
          case _                    => AddNotificationResponse()
        }
      case _ => Future.successful(AddNotificationResponse())
    }
  }

  override def addSMS(in: AddSMSRequest): Future[AddNotificationResponse] = {
    in.sms match {
      case Some(sms) =>
        ?(sms.uuid, AddNotification(sms)) map {
          case _: NotificationAdded => AddNotificationResponse(true)
          case _                    => AddNotificationResponse()
        }
      case _ => Future.successful(AddNotificationResponse())
    }
  }

  override def addPush(in: AddPushRequest): Future[AddNotificationResponse] = {
    in.push match {
      case Some(push) =>
        ?(push.uuid, AddNotification(push)) map {
          case _: NotificationAdded => AddNotificationResponse(true)
          case _                    => AddNotificationResponse()
        }
      case _ => Future.successful(AddNotificationResponse())
    }
  }
}
