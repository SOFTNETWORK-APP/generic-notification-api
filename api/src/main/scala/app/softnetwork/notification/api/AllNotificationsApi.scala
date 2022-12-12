package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.AllNotificationsHandler
import app.softnetwork.notification.launch.NotificationApplication
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.persistence.query.{
  NotificationCommandProcessorStream,
  Scheduler2NotificationProcessorStream
}
import app.softnetwork.notification.persistence.typed.{
  AllNotificationsBehavior,
  NotificationBehavior
}
import app.softnetwork.persistence.jdbc.query.{JdbcJournalProvider, JdbcSchema, JdbcSchemaProvider}
import app.softnetwork.scheduler.api.SchedulerApi

trait AllNotificationsApi extends SchedulerApi with NotificationApplication[Notification] {

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Notification]] =
    _ => Seq(AllNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream()
          with AllNotificationsHandler
          with JdbcJournalProvider
          with JdbcSchemaProvider {
          override val tag = s"${AllNotificationsBehavior.persistenceId}-scheduler"
          override lazy val schemaType: JdbcSchema.SchemaType = jdbcSchemaType
          override implicit val system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with AllNotificationsHandler
          with JdbcJournalProvider
          with JdbcSchemaProvider {
          override lazy val schemaType: JdbcSchema.SchemaType = jdbcSchemaType
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] = sys =>
    Seq(AllNotificationsServer(sys))

}
