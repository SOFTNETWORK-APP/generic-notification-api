package app.softnetwork.notification.persistence.query

import akka.actor.typed.eventstream.EventStream.Publish
import app.softnetwork.scheduler.model.Schedule
import app.softnetwork.persistence.query.{JournalProvider, OffsetProvider}
import app.softnetwork.scheduler.persistence.query.Scheduler2EntityProcessorStream
import app.softnetwork.notification.audit.NotificationAuditLog._
import app.softnetwork.notification.handlers.NotificationHandler
import app.softnetwork.notification.message._

import scala.concurrent.Future

/** Created by smanciot on 04/09/2020.
  */
trait Scheduler2NotificationProcessorStream
    extends Scheduler2EntityProcessorStream[NotificationCommand, NotificationCommandResult] {
  _: JournalProvider with OffsetProvider with NotificationHandler =>

  override protected def triggerSchedule(schedule: Schedule): Future[Boolean] = {
    val cid = schedule.correlationId.getOrElse(
      s"schedule#${schedule.persistenceId}#${schedule.entityId}#${schedule.key}"
    )
    val cmd = TriggerSchedule4Notification(schedule)
    cmd.withCorrelationId(cid)
    !?(cmd) map {
      case result: Schedule4NotificationTriggered =>
        audit.event(
          cid,
          "schedule_fired",
          "actor"        -> "scheduler",
          "schedule_key" -> schedule.key
        )
        if (forTests) {
          system.eventStream.tell(Publish(result))
        }
        true
      case Schedule4NotificationNotTriggered =>
        audit.event(
          cid,
          "schedule_not_fired",
          "actor"        -> "scheduler",
          "schedule_key" -> schedule.key
        )
        false
      case _ =>
        audit.event(
          cid,
          "schedule_failed",
          "actor"        -> "scheduler",
          "schedule_key" -> schedule.key
        )
        false
    }
  }
}
