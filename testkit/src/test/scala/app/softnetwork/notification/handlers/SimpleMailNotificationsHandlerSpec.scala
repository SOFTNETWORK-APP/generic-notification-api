package app.softnetwork.notification.handlers

import app.softnetwork.notification.message._
import app.softnetwork.notification.model.Attachment
import app.softnetwork.notification.scalatest.SimpleMailNotificationsTestKit
import app.softnetwork.persistence.audit.AuditLog
import ch.qos.logback.classic.{Logger => LogbackLogger}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.{Logger, LoggerFactory}

import java.nio.file.Paths

/** Created by smanciot on 14/04/2020.
  */
class SimpleMailNotificationsHandlerSpec
    extends SimpleMailNotificationsHandler
    with AnyWordSpecLike
    with SimpleMailNotificationsTestKit {

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
      assert(client.addMail(generateMail(uuid)).complete())
      assert(probe.receiveMessage().schedule.uuid == s"MailNotification#$uuid#NotificationTimerKey")
    }

    "emit a notification_sent audit line carrying the correlation id (Story 13.7)" in {
      val cid = "notif-corr-13-7"
      val auditLogger = LoggerFactory.getLogger(AuditLog.LoggerName).asInstanceOf[LogbackLogger]
      val appender = new ListAppender[ILoggingEvent]()
      appender.start()
      auditLogger.addAppender(appender)
      try {
        val uuid = "auditedSend"
        this ? (uuid, SendNotification(generateMail(uuid).withCorrelationId(cid))) await {
          case n: NotificationSent => n.uuid shouldBe uuid
          case _                   => fail()
        }
        // the emission runs in the behavior's thenRun BEFORE the reply, so it is captured by now
        val sentLine =
          appender.list.toArray.toList.collect { case e: ILoggingEvent => e }.find { e =>
            val fields = e.getArgumentArray.map(_.toString).toSet
            fields.contains("event_type=notification_sent") && fields.contains(
              s"correlation_id=$cid"
            )
          }
        assert(sentLine.isDefined, "expected a notification_sent audit line carrying the cid")
        sentLine.get.getArgumentArray.map(_.toString) should contain("service=notification")
      } finally {
        auditLogger.detachAppender(appender)
        appender.stop()
      }
    }

  }

}
