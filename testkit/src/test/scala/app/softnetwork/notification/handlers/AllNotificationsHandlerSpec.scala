package app.softnetwork.notification.handlers

import org.scalatest.wordspec.AnyWordSpecLike
import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.AllNotificationsTestKit

/** Created by smanciot on 14/04/2020.
  */
class AllNotificationsHandlerSpec
    extends AllNotificationsHandler
    with AnyWordSpecLike
    with AllNotificationsTestKit {

  "Notification handler" must {

    "add notification" in {
      val uuid = "add"
      this ? (uuid, AddNotification(generateMail(uuid))) await {
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
      this ? (uuid, AddNotification(generateMail(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey"
          )
          this ? (uuid, RemoveNotification(uuid)) await {
            case _: NotificationRemoved.type => succeed
            case _                           => fail()
          }
        case _ => fail()
      }
    }

    "send notification" in {
      val uuid = "send"
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent => n.uuid shouldBe uuid
        case _                   => fail()
      }
    }

    "resend notification" in {
      val uuid = "resend"
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, ResendNotification(uuid)) await {
            case n: NotificationSent => n.uuid shouldBe uuid
            case _                   => fail()
          }
          this ? ("fake", ResendNotification(uuid)) await {
            case NotificationNotFound => succeed
            case _                    => fail()
          }
        case _ => fail()
      }
    }

    "retrieve notification status" in {
      val uuid = "status"
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent => n.uuid shouldBe uuid
            case _                   => fail()
          }
        case _ => fail()
      }
    }

    "trigger notification" in {
      val uuid = "trigger"
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, GetNotificationStatus(uuid)) await {
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

    "add mail" in {
      val uuid = "mail"
      assert(client.addMail(generateMail(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey")
    }

    "add sms" in {
      val uuid = "sms"
      assert(client.addSMS(generateSMS(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey")
    }

    "add push" in {
      val uuid = "push"
      assert(client.addPush(generatePush(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"Notification#$uuid#NotificationTimerKey")
    }
  }
}
