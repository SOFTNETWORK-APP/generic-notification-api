package app.softnetwork.notification

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.model.{
  Mail,
  NotificationAck,
  NotificationStatus,
  NotificationStatusResult,
  Platform,
  Push,
  PushPayload,
  SMS
}

import java.time.Instant
import java.util.Date
import scala.language.implicitConversions

package object spi {

  trait NotificationProvider {
    type N <: Notification

    def send(notification: N)(implicit system: ActorSystem[_]): NotificationAck

    def !(notification: N)(implicit system: ActorSystem[_]): NotificationAck = send(notification)

    def ack(notification: N)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())

    def ?(notification: N)(implicit system: ActorSystem[_]): NotificationAck = ack(notification)
  }

  trait MailProvider {
    def sendMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck
    def ackMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  trait SMSProvider {
    def sendSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck
    def ackSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  trait PushProvider {
    def bulkSize = 100
    def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck
    def ackPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  implicit def toPushPayload(notification: Push): PushPayload = {
    PushPayload.defaultInstance
      .withApplication(
        notification.application.getOrElse(
          notification.from.alias.getOrElse(notification.from.value)
        )
      )
      .withTitle(notification.subject)
      .withBody(notification.message)
      .withBadge(notification.badge)
      .copy(sound = notification.sound)
  }

  trait AndroidProvider extends PushProvider {
    def pushToAndroid(payload: PushPayload, devices: Seq[String])(implicit
      system: ActorSystem[_]
    ): Seq[NotificationStatusResult]
    override def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck = {
      NotificationAck(
        None,
        pushToAndroid(
          notification,
          notification.devices.filter(_.platform == Platform.ANDROID).map(_.regId).distinct
        ).distinct ++
        notification.devices
          .filterNot(_.platform == Platform.ANDROID)
          .map(d =>
            NotificationStatusResult(
              d.regId,
              NotificationStatus.Undelivered,
              Some(s"${d.platform.name} device not handled by AndroidProvider")
            )
          ),
        Instant.now()
      )
    }
  }

  trait IosProvider extends PushProvider {
    def pushToIos(payload: PushPayload, devices: Seq[String])(implicit
      system: ActorSystem[_]
    ): Seq[NotificationStatusResult]
    override def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck = {
      NotificationAck(
        None,
        pushToIos(
          notification,
          notification.devices.filter(_.platform == Platform.IOS).map(_.regId).distinct
        ).distinct ++
        notification.devices
          .filterNot(_.platform == Platform.IOS)
          .map(d =>
            NotificationStatusResult(
              d.regId,
              NotificationStatus.Undelivered,
              Some(s"${d.platform.name} device not handled by IosProvider")
            )
          ),
        Instant.now()
      )
    }
  }

  trait AndroidAndIosProvider extends PushProvider with AndroidProvider with IosProvider {
    final override def sendPush(
      notification: Push
    )(implicit system: ActorSystem[_]): NotificationAck = {
      // split notification per platform
      val (android, ios) = notification.devices.partition(_.platform == Platform.ANDROID)
      // send notification to devices per platform
      NotificationAck(
        None,
        pushToIos(notification, ios.map(_.regId).distinct) ++ pushToAndroid(
          notification,
          android.map(_.regId)
        ).distinct,
        Instant.now()
      )
    }
  }

  trait MailAndSMSAndFcmAndIosProvider
      extends NotificationProvider
      with MailProvider
      with SMSProvider
      with AndroidAndIosProvider {
    override type N = Notification

    override def send(
      notification: Notification
    )(implicit system: ActorSystem[_]): NotificationAck = notification match {
      case mail: Mail => sendMail(mail)
      case sms: SMS   => sendSMS(sms)
      case push: Push => sendPush(push)
      case _          => NotificationAck(notification.ackUuid, notification.results, Instant.now())
    }

    override def ack(notification: Notification)(implicit system: ActorSystem[_]): NotificationAck =
      notification match {
        case mail: Mail => ackMail(mail)
        case sms: SMS   => ackSMS(sms)
        case push: Push => ackPush(push)
        case _ => NotificationAck(notification.ackUuid, notification.results, Instant.now())
      }
  }

}
