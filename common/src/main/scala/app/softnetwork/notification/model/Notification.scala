package app.softnetwork.notification.model

import app.softnetwork.persistence.model.State

import java.time.Instant
import scala.language.reflectiveCalls

/** Created by smanciot on 14/04/2018.
  */
trait Notification extends State with NotificationDecorator {
  def uuid: String
  def createdDate: Instant
  def lastUpdated: Instant
  def from: From
  def to: Seq[String]
  def subject: String
  def message: String
  def `type`: NotificationType
  def maxTries: Int
  def nbTries: Int
  def deferred: Option[Instant]

  def ackUuid: Option[String]
  def status: NotificationStatus

  def results: Seq[NotificationStatusResult]

  def removeOnSuccess: Option[Boolean] = None

  def removeAfterMaxTries: Option[Boolean] = None
}

trait NotificationDecorator { _: Notification =>

  def withTo(to: Seq[String]): Notification with NotificationDecorator

  def withNbTries(nbTries: Int): Notification with NotificationDecorator

  def withAckUuid(ackUuid: String): Notification with NotificationDecorator

  def withStatus(status: NotificationStatus): Notification with NotificationDecorator

  def withResults(results: Seq[NotificationStatusResult]): Notification with NotificationDecorator

  def withLastUpdated(lastUpdated: Instant): Notification with NotificationDecorator

  def incNbTries(): Notification with NotificationDecorator = withNbTries(nbTries + 1)

  def copyWithAck(ack: NotificationAck): Notification =
    withAckUuid(ack.uuid.orNull)
      .withResults(ack.results)
      .withStatus(NotificationAckDecorator.status(ack))
      .withTo(
        to.filterNot(t =>
          ack.results.find(_.recipient == t).exists(r => r.status.isDelivered || r.status.isSent)
        )
      )
      .withLastUpdated(ack.date)
}

trait NotificationAckDecorator { _: NotificationAck =>
  def status: NotificationStatus = NotificationAckDecorator.status(this)
}

object NotificationAckDecorator {
  def status(ack: NotificationAck): NotificationStatus = {
    val distinct = ack.results.map(_.status).distinct
    if (distinct.contains(NotificationStatus.Rejected)) {
      NotificationStatus.Rejected
    } else if (distinct.contains(NotificationStatus.Undelivered)) {
      NotificationStatus.Undelivered
    } else if (distinct.contains(NotificationStatus.Pending)) {
      NotificationStatus.Pending
    } else if (distinct.contains(NotificationStatus.Sent)) {
      NotificationStatus.Sent
    } else if (distinct.contains(NotificationStatus.Delivered)) {
      NotificationStatus.Delivered
    } else {
      NotificationStatus.UnknownNotificationStatus
    }
  }
}
