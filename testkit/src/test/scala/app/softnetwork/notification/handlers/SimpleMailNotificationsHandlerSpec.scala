package app.softnetwork.notification.handlers

import app.softnetwork.notification.message._
import app.softnetwork.notification.model.Attachment
import app.softnetwork.notification.scalatest.SimpleMailNotificationsTestKit
import app.softnetwork.session.handlers.SessionRefreshTokenDao
import app.softnetwork.session.service.BasicSessionMaterials
import com.softwaremill.session.RefreshTokenStorage
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.{Logger, LoggerFactory}
import org.softnetwork.session.model.Session

import java.nio.file.Paths

/** Created by smanciot on 14/04/2020.
  */
class SimpleMailNotificationsHandlerSpec
    extends SimpleMailNotificationsHandler
    with AnyWordSpecLike
    with SimpleMailNotificationsTestKit
    with BasicSessionMaterials[Session] {

  override implicit def refreshTokenStorage: RefreshTokenStorage[Session] = SessionRefreshTokenDao(
    ts
  )

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  var attachment: Attachment = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    attachment = generateAttachment(
      "avatar.png",
      Paths.get(Thread.currentThread().getContextClassLoader.getResource("avatar.png").getPath)
    )
  }

  "Simple Mail Notification handler" must {

    "add notification" in {
      val uuid = "add"
      this ? (uuid, AddNotification(generateMail(uuid))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"MailNotification#$uuid#NotificationTimerKey"
          )
        case _ => fail()
      }
    }

    "add notification with attachment(s)" in {
      val uuid = "addWithAttachments"
      this ? (uuid, AddNotification(generateMail(uuid, Seq(attachment)))) await {
        case n: NotificationAdded =>
          n.uuid shouldBe uuid
          assert(
            probe.receiveMessage().schedule.uuid == s"MailNotification#$uuid#NotificationTimerKey"
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
            probe.receiveMessage().schedule.uuid == s"MailNotification#$uuid#NotificationTimerKey"
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
        case n: NotificationSent =>
          assert(n.uuid == uuid)
        case _ => fail()
      }
    }

    "send notification with attachment(s)" in {
      val uuid = "sendWithAttachments"
      this ? (uuid, SendNotification(generateMail(uuid, Seq(attachment)))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
        case _ => fail()
      }
    }

    "resend notification" in {
      val uuid = "resend"
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, ResendNotification(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
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
      this ? (uuid, SendNotification(generateMail(uuid))) await {
        case n: NotificationSent =>
          n.uuid shouldBe uuid
          this ? (uuid, GetNotificationStatus(uuid)) await {
            case n: NotificationSent =>
              assert(n.uuid == uuid)
            case _ => fail()
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
              assert(n.uuid == uuid)
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
      assert(probe.receiveMessage().schedule.uuid == s"MailNotification#$uuid#NotificationTimerKey")
    }

  }

}
