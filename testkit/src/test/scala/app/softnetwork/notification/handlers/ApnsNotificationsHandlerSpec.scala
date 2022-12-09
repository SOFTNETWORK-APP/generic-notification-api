package app.softnetwork.notification.handlers

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.eventstream.EventStream.Subscribe
import app.softnetwork.notification.api.{NotificationClient, NotificationGrpcServer}
import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.ApnsNotificationsTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.softnetwork.notification.model.{BasicDevice, NotificationStatus, Platform, Push}

/** Created by smanciot on 07/12/2022.
  */
class ApnsNotificationsHandlerSpec
    extends ApnsNotificationsHandler
    with AnyWordSpecLike
    with NotificationGrpcServer[Push]
    with ApnsNotificationsTestKit {

  val subject = "test"
  val message = "message"

  protected def generatePush(uuid: String, devices: BasicDevice*): Push =
    Push.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)
      .withDevices(devices)
      .withApp("mock")

  val iosDevice: BasicDevice = BasicDevice(generateRandomDeviceToken, Platform.IOS)

  val androidDevice: BasicDevice = BasicDevice("test-token-android", Platform.ANDROID)

  val probe: TestProbe[Schedule4NotificationTriggered] =
    createTestProbe[Schedule4NotificationTriggered]()
  system.eventStream.tell(Subscribe(probe.ref))

  lazy val client: NotificationClient = NotificationClient(system)

  "Apns Notification handler" must {

    "add notification" in {
      val uuid = "add"
      this ? (uuid, AddNotification(generatePush(uuid, iosDevice))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"IosNotification#$uuid#NotificationTimerKey"
          )
        case _ => fail()
      }
    }

    "remove notification" in {
      val uuid = "remove"
      this ? (uuid, AddNotification(generatePush(uuid, iosDevice))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"IosNotification#$uuid#NotificationTimerKey"
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
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice))) await {
        case n: NotificationSent =>
          assert(n.uuid == uuid)
          assert(
            n.results
              .find(_.recipient == iosDevice.regId)
              .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
          )
        case _ => fail()
      }
    }

    "not send notification to only android device(s)" in {
      val uuid = "android"
      this ? (uuid, SendNotification(generatePush(uuid, androidDevice))) await {
        case n: NotificationUndelivered =>
          assert(n.uuid == uuid)
          assert(
            n.results
              .find(_.recipient == androidDevice.regId)
              .exists(_.status == NotificationStatus.Undelivered)
          )
        case other => fail(other.toString)
      }
    }

    "send notification for android and ios devices" in {
      val uuid = "android_ios"
      this ? (uuid, SendNotification(generatePush(uuid, androidDevice, iosDevice))) await {
        case n: NotificationUndelivered =>
          assert(n.uuid == uuid)
          assert(
            n.results
              .find(_.recipient == androidDevice.regId)
              .exists(_.status == NotificationStatus.Undelivered)
          )
          assert(
            n.results
              .find(_.recipient == iosDevice.regId)
              .exists(r => r.status == NotificationStatus.Sent && r.uuid.isDefined)
          )
        case other => fail(other.toString)
      }
    }

    "resend notification" in {
      val uuid = "resend"
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice))) await {
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
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice))) await {
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
            case _ => fail()
          }
        case _ => fail()
      }
    }

    "trigger notification" in {
      val uuid = "trigger"
      this ? (uuid, SendNotification(generatePush(uuid, iosDevice))) await {
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
      assert(client.addPush(generatePush(uuid, iosDevice)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"IosNotification#$uuid#NotificationTimerKey")
    }
  }
}
