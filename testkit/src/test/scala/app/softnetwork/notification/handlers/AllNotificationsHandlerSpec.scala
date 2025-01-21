package app.softnetwork.notification.handlers

import org.scalatest.wordspec.AnyWordSpecLike
import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.AllNotificationsTestKit
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success}

/** Created by smanciot on 14/04/2020.
  */
class AllNotificationsHandlerSpec extends AnyWordSpecLike with AllNotificationsTestKit {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  "Notification handler" must {

    "add notification" in {
      val uuid = "add"
      allNotificationsHandler ? (uuid, AddNotification(generateMail(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
          )
        case _ => fail()
      }
    }

    "remove notification" in {
      val uuid = "remove"
      allNotificationsHandler ? (uuid, AddNotification(generateMail(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
          )
          allNotificationsHandler ? (uuid, RemoveNotification(uuid)) await {
            case _: NotificationRemoved.type => succeed
            case _                           => fail()
          }
        case _ => fail()
      }
    }

    "send notification" in {
      val uuid = "send"
      allNotificationsHandler ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent => n.uuid shouldBe uuid
        case _                   => fail()
      }
    }

    "resend notification" in {
      val uuid = "resend"
      allNotificationsHandler ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          allNotificationsHandler ? (uuid, ResendNotification(uuid)) await {
            case n: NotificationSent => n.uuid shouldBe uuid
            case _                   => fail()
          }
          allNotificationsHandler ? ("fake", ResendNotification(uuid)) await {
            case NotificationNotFound => succeed
            case _                    => fail()
          }
        case _ => fail()
      }
    }

    "retrieve notification status" in {
      val uuid = "status"
      allNotificationsHandler ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          allNotificationsHandler ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent => n.uuid shouldBe uuid
            case _                   => fail()
          }
        case _ => fail()
      }
    }

    "trigger notification" in {
      val uuid = "trigger"
      allNotificationsHandler ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          allNotificationsHandler ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              n.uuid shouldBe uuid
              succeed
            case _ =>
              probe.expectMessageType[Schedule4NotificationTriggered]
              succeed
          }
        case _ => fail()
      }
    }

    "add mail using client" in {
      val uuid = "mail"
      assert(client.addMail(generateMail(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey")
    }

    "send mail using client" in {
      val uuid = "mail2"
      val mail = generateMail(uuid)
      client.sendMail(mail) complete () match {
        case Success(result) =>
          assert(result.exists(r => r.recipient == mail.to.head && r.status.isSent))
        case Failure(_) => fail()
      }
    }

    "remove notification using client" in {
      assert(client.removeNotification("mail") complete ())
    }

    "add sms using client" in {
      val uuid = "sms"
      assert(client.addSMS(generateSMS(uuid)) complete ())
      assert(
        probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
      ) // pending
      assert(
        probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
      ) // ack
    }

    "send sms using client" in {
      val uuid = "sms2"
      val sms = generateSMS(uuid)
      client.sendSMS(sms) complete () match {
        case Success(result) =>
          assert(result.exists(r => r.recipient == sms.to.head && r.status.isPending))
          assert(
            probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
          ) // ack
        case Failure(_) => fail()
      }
    }

    "add push using client" in {
      val uuid = "push"
      assert(client.addPush(generatePush(uuid, androidDevice, iosDevice)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey")
    }

    "retrieve push notification status using client" in {
      client.getNotificationStatus("push") complete () match {
        case Success(result) =>
          assert(result.exists(r => r.recipient == androidDevice.regId && r.status.isSent))
          assert(result.exists(r => r.recipient == iosDevice.regId)) // FIXME && r.status.isSent
        case Failure(_) => fail()
      }
    }

    "send push using client" in {
      val uuid = "push2"
      val push = generatePush(uuid, androidDevice, iosDevice)
      client.sendPush(push) complete () match {
        case Success(result) =>
          assert(result.exists(r => r.recipient == androidDevice.regId && r.status.isSent))
          assert(result.exists(r => r.recipient == iosDevice.regId && r.status.isSent))
        case Failure(_) => fail()
      }
    }
  }
}
