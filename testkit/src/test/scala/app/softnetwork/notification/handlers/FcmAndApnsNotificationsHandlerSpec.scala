package app.softnetwork.notification.handlers

import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.FcmAndApnsNotificationsTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import app.softnetwork.notification.model.NotificationStatus

/** Created by smanciot on 07/12/2022.
  */
class FcmAndApnsNotificationsHandlerSpec
    extends FcmAndApnsNotificationsHandler
    with AnyWordSpecLike
    with FcmAndApnsNotificationsTestKit {

  "Fcm and Apns Notification handler" must {

    "add notification" in {
      val uuid = "add"
      this ? (uuid, AddNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe
              .receiveMessage()
              .schedule
              .uuid == s"AndroidAndIosNotification#$uuid#NotificationTimerKey"
          )
        case _ => fail()
      }
    }

    "remove notification" in {
      val uuid = "remove"
      this ? (uuid, AddNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe
              .receiveMessage()
              .schedule
              .uuid == s"AndroidAndIosNotification#$uuid#NotificationTimerKey"
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
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationSent =>
          assert(n.uuid == uuid)
          assert(
            n.results
              .find(_.recipient == iosDevice.regId)
              .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
          )
          assert(
            n.results
              .find(_.recipient == androidDevice.regId)
              .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
          )
        case _ => fail()
      }
    }

    "resend notification" in {
      val uuid = "resend"
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, ResendNotification(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
              assert(
                n.results
                  .find(_.recipient == iosDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
              assert(
                n.results
                  .find(_.recipient == androidDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
            case _ => fail()
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
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
              assert(
                n.results
                  .find(_.recipient == iosDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
              assert(
                n.results
                  .find(_.recipient == androidDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
            case _ => fail()
          }
        case _ => fail()
      }
    }

    "trigger notification" in {
      val uuid = "trigger"
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice, androidDevice))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
              assert(
                n.results
                  .find(_.recipient == iosDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
              assert(
                n.results
                  .find(_.recipient == androidDevice.regId)
                  .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
              )
              succeed
            case _ =>
              probe.expectMessageType[Schedule4NotificationTriggered]
              succeed
          }
        case _ => fail()
      }
    }

    "add push" in {
      val uuid = "push"
      assert(client.addPush(generatePush(uuid, iosDevice, androidDevice)) complete ())
      assert(
        probe
          .receiveMessage()
          .schedule
          .uuid == s"AndroidAndIosNotification#$uuid#NotificationTimerKey"
      )
    }
  }
}
