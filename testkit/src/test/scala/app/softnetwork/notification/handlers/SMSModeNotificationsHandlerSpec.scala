package app.softnetwork.notification.handlers

import app.softnetwork.notification.message._
import app.softnetwork.notification.scalatest.SMSModeNotificationsTestKit
import app.softnetwork.session.service.BasicSessionMaterials
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.{Logger, LoggerFactory}

/** Created by smanciot on 07/12/2022.
  */
class SMSModeNotificationsHandlerSpec
    extends SMSModeNotificationsHandler
    with AnyWordSpecLike
    with SMSModeNotificationsTestKit
    with BasicSessionMaterials {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

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

    "add sms" in {
      val uuid = "sms"
      assert(client.addSMS(generateSMS(uuid)) complete ())
      assert(probe.receiveMessage().schedule.uuid == s"SMSNotification#$uuid#NotificationTimerKey")
    }
  }
}
