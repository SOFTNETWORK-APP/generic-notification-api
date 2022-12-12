package app.softnetwork.notification.handlers

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream.Subscribe
import app.softnetwork.notification.api.NotificationClient
import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.SMSModeNotificationsTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.softnetwork.notification.model.SMS

/** Created by smanciot on 07/12/2022.
  */
class SMSModeNotificationsHandlerSpec
    extends SMSModeNotificationsHandler
    with AnyWordSpecLike
    with SMSModeNotificationsTestKit {

  val subject = "test"
  val message = "message"

  implicit lazy val system: ActorSystem[_] = typedSystem()

  private[this] def generateSMS(uuid: String): SMS =
    SMS.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)
      .withTo(Seq(uuid))

  val probe: TestProbe[Schedule4NotificationTriggered] =
    createTestProbe[Schedule4NotificationTriggered]()
  system.eventStream.tell(Subscribe(probe.ref))

  lazy val client: NotificationClient = NotificationClient(system)

  "SMS Notification handler" must {

    "add notification" in {
      val uuid = "add"
      ?(uuid, AddNotification(generateSMS(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          // second call to retrieve the SMS ack
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
        case _ => fail()
      }
    }

    "remove notification" in {
      val uuid = "remove"
      ?(uuid, AddNotification(generateSMS(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          ?(uuid, RemoveNotification(uuid)) await {
            case _: NotificationRemoved.type => succeed
            case _                           => fail()
          }
        case _ => fail()
      }
    }

    "send notification" in {
      val uuid = "send"
      ?(uuid, SendNotification(generateSMS(uuid))) await {
        case n: NotificationPending => // SMS should have been accepted
          assert(n.uuid == uuid)
          // waiting for a call to get the SMS ack
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          ?(uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
            case _ => fail()
          }
        case other => fail(other.getClass.toString)
      }
    }

    "resend notification" in {
      val uuid = "resend"
      ?(uuid, SendNotification(generateSMS(uuid))) await {
        case n: NotificationPending =>
          assert(n.uuid == uuid)
          // waiting for a call to get the SMS ack
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          ?(uuid, ResendNotification(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
            case _ => fail()
          }
          ?("fake", ResendNotification(uuid)) await {
            case NotificationNotFound => succeed
            case _                    => fail()
          }
        case _ => fail()
      }
    }

    "retrieve notification status" in {
      val uuid = "status"
      ?(uuid, SendNotification(generateSMS(uuid))) await {
        case n: NotificationPending =>
          assert(n.uuid == uuid)
          // waiting for a call to get the SMS ack
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          ?(uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
            case _ => fail()
          }
        case _ => fail()
      }
    }

    "trigger notification" in {
      val uuid = "trigger"
      ?(uuid, SendNotification(generateSMS(uuid))) await {
        case n: NotificationPending =>
          assert(n.uuid == uuid)
          // waiting for a call to get the SMS ack
          assert(
            probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey"
          )
          ?(uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
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
      assert(client.addSMS(generateSMS(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey")
    }
  }
}
