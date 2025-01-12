package app.softnetwork.notification

import app.softnetwork.persistence.message._
import app.softnetwork.scheduler.model.Schedule
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.model.NotificationStatusResult

/** Created by smanciot on 15/04/2020.
  */
package object message {

  sealed trait NotificationCommand extends EntityCommand

  @SerialVersionUID(0L)
  case object ScheduleNotification extends NotificationCommand with AllEntities

  @SerialVersionUID(0L)
  case class AddNotification[T <: Notification](notification: T) extends NotificationCommand {
    override val id: String = notification.uuid
  }

  @SerialVersionUID(0L)
  case class RemoveNotification(id: String) extends NotificationCommand

  @SerialVersionUID(0L)
  case class SendNotification[T <: Notification](notification: T) extends NotificationCommand {
    override val id: String = notification.uuid
  }

  @SerialVersionUID(0L)
  case class ResendNotification(id: String) extends NotificationCommand

  @SerialVersionUID(0L)
  case class GetNotificationStatus(id: String) extends NotificationCommand

  case class TriggerSchedule4Notification(schedule: Schedule) extends NotificationCommand {
    override val id: String = schedule.entityId
  }

  sealed trait NotificationCommandResult extends CommandResult

  @SerialVersionUID(0L)
  case class NotificationAdded(uuid: String) extends NotificationCommandResult

  case object NotificationRemoved extends NotificationCommandResult

  trait NotificationResults extends NotificationCommandResult {
    def results: Seq[NotificationStatusResult]
  }

  @SerialVersionUID(0L)
  case class NotificationSent(uuid: String, results: Seq[NotificationStatusResult])
      extends NotificationResults

  @SerialVersionUID(0L)
  case class NotificationDelivered(uuid: String, results: Seq[NotificationStatusResult])
      extends NotificationResults

  @SerialVersionUID(0L)
  case class NotificationPending(uuid: String, results: Seq[NotificationStatusResult])
      extends NotificationResults

  case class Schedule4NotificationTriggered(schedule: Schedule) extends NotificationCommandResult

  @SerialVersionUID(0L)
  class NotificationErrorMessage(override val message: String)
      extends ErrorMessage(message)
      with NotificationCommandResult

  @SerialVersionUID(0L)
  case class NotificationUndelivered(uuid: String, results: Seq[NotificationStatusResult])
      extends NotificationErrorMessage("NotificationUndelivered")
      with NotificationResults

  @SerialVersionUID(0L)
  case class NotificationRejected(uuid: String, results: Seq[NotificationStatusResult])
      extends NotificationErrorMessage("NotificationRejected")
      with NotificationResults

  case object NotificationNotFound extends NotificationErrorMessage("NotificationNotFound")

  case object NotificationNotAdded extends NotificationErrorMessage("NotificationNotAdded")

  case object NotificationMaxTriesReached
      extends NotificationErrorMessage("NotificationMaxTriesReached")

  case object NotificationUnknownCommand
      extends NotificationErrorMessage("NotificationUnknownCommand")

  case object Schedule4NotificationNotTriggered
      extends NotificationErrorMessage("Schedule4NotificationNotTriggered")

  trait NotificationRecordedEventDecorator { _: NotificationRecordedEvent =>
    def notification: Option[Notification] =
      wrapped match {
        case r: NotificationRecordedEvent.Wrapped.Mail => Some(r.value)
        case r: NotificationRecordedEvent.Wrapped.Sms  => Some(r.value)
        case r: NotificationRecordedEvent.Wrapped.Push => Some(r.value)
        case r: NotificationRecordedEvent.Wrapped.Ws   => Some(r.value)
        case _                                         => None
      }
  }

  trait ExternalEntityToNotificationEventDecorator extends NotificationCommandEvent {
    _: ExternalEntityToNotificationEvent =>
    override def command: Option[NotificationCommand] =
      wrapped match {
        case r: ExternalEntityToNotificationEvent.Wrapped.AddMail =>
          Some(AddNotification(r.value.notification))
        case r: ExternalEntityToNotificationEvent.Wrapped.AddSMS =>
          Some(AddNotification(r.value.notification))
        case r: ExternalEntityToNotificationEvent.Wrapped.AddPush =>
          Some(AddNotification(r.value.notification))
        case r: ExternalEntityToNotificationEvent.Wrapped.RemoveNotification =>
          Some(RemoveNotification(r.value.uuid))
        case r: ExternalEntityToNotificationEvent.Wrapped.AddWs =>
          Some(AddNotification(r.value.notification))
        case _ => None
      }
  }
}
