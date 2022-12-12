package app.softnetwork.notification.handlers

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.eventstream.EventStream.Subscribe
import app.softnetwork.notification.api.NotificationClient
import org.scalatest.wordspec.AnyWordSpecLike
import app.softnetwork.notification.config.MailSettings
import app.softnetwork.notification.message._
import org.softnetwork.notification.model.{From, Mail, Push, SMS}
import app.softnetwork.notification.scalatest.AllNotificationsTestKit

/** Created by smanciot on 14/04/2020.
  */
class AllNotificationsHandlerSpec
    extends AllNotificationsHandler
    with AnyWordSpecLike
    with AllNotificationsTestKit {

  lazy val from: String = MailSettings.MailConfig.username
  val to = Seq("nobody@gmail.com")
  val subject = "test"
  val message = "message"

  private[this] def generateMail(uuid: String): Mail =
    Mail.defaultInstance
      .withUuid(uuid)
      .withFrom(From(from, None))
      .withTo(to)
      .withSubject(subject)
      .withMessage(message)

  private[this] def generateSMS(uuid: String): SMS =
    SMS.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)

  private[this] def generatePush(uuid: String): Push =
    Push.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)

  val probe: TestProbe[Schedule4NotificationTriggered] =
    createTestProbe[Schedule4NotificationTriggered]()
  system.eventStream.tell(Subscribe(probe.ref))

  lazy val client: NotificationClient = NotificationClient(system)

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
