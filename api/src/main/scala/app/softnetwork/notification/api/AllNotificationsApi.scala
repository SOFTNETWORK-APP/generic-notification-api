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
import app.softnetwork.persistence.jdbc.query.{JdbcJournalProvider, JdbcOffsetProvider}
import app.softnetwork.persistence.jdbc.schema.JdbcSchemaProvider
import app.softnetwork.scheduler.config.SchedulerSettings
import com.typesafe.config.Config

trait AllNotificationsApi extends NotificationApplication[Notification] with JdbcSchemaProvider {

  override def notificationBehaviors: ActorSystem[_] => Seq[NotificationBehavior[Notification]] =
    _ => Seq(AllNotificationsBehavior)

  override def scheduler2NotificationProcessorStream
    : ActorSystem[_] => Option[Scheduler2NotificationProcessorStream] =
    sys =>
      Some(
        new Scheduler2NotificationProcessorStream()
          with AllNotificationsHandler
          with JdbcJournalProvider
          with JdbcOffsetProvider {
          override def config: Config = AllNotificationsApi.this.config

          override val tag: String = SchedulerSettings.tag(AllNotificationsBehavior.persistenceId)
          override implicit def system: ActorSystem[_] = sys
        }
      )

  override def notificationCommandProcessorStream
    : ActorSystem[_] => Option[NotificationCommandProcessorStream] =
    sys =>
      Some(
        new NotificationCommandProcessorStream
          with AllNotificationsHandler
          with JdbcJournalProvider
          with JdbcOffsetProvider {
          override def config: Config = AllNotificationsApi.this.config
          override implicit def system: ActorSystem[_] = sys
        }
      )

  /** initialize all notification servers
    */
  override def notificationServers: ActorSystem[_] => Seq[NotificationServer] = sys =>
    Seq(AllNotificationsServer(sys))

}
