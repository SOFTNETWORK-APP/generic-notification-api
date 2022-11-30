package app.softnetwork.notification

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.model.Notification
import org.softnetwork.notification.model.{
  Mail,
  NotificationAck,
  NotificationStatusResult,
  Platform,
  Push,
  PushPayload,
  SMS
}

import java.util.Date
import scala.language.implicitConversions

package object spi {

  trait NotificationProvider {
    type N <: Notification

    def send(notification: N)(implicit system: ActorSystem[_]): NotificationAck

    def !(notification: N)(implicit system: ActorSystem[_]): NotificationAck = send(notification)

    def ack(notification: N)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, new Date())

    def ?(notification: N)(implicit system: ActorSystem[_]): NotificationAck = ack(notification)
  }

  trait MailProvider {
    def sendMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck
    def ackMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, new Date())
  }

  trait SMSProvider {
    def sendSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck
    def ackSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, new Date())
  }

  trait PushProvider {
    def bulkSize = 100
    def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck
    def ackPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, new Date())
  }

  implicit def toPushPayload(notification: Push): PushPayload = {
    PushPayload.defaultInstance
      .withApp(
        notification.app.getOrElse(notification.from.alias.getOrElse(notification.from.value))
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
        ).distinct,
        new Date()
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
        ).distinct,
        new Date()
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
        new Date()
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
      case _          => NotificationAck(notification.ackUuid, notification.results, new Date())
    }

    override def ack(notification: Notification)(implicit system: ActorSystem[_]): NotificationAck =
      notification match {
        case mail: Mail => ackMail(mail)
        case sms: SMS   => ackSMS(sms)
        case push: Push => ackPush(push)
        case _          => NotificationAck(notification.ackUuid, notification.results, new Date())
      }
  }

}
