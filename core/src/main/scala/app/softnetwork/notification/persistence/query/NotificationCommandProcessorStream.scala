package app.softnetwork.notification.persistence.query

import akka.Done
import akka.actor.typed.eventstream.EventStream.Publish
import akka.persistence.typed.PersistenceId
import app.softnetwork.notification.config.NotificationSettings
import app.softnetwork.notification.handlers.NotificationHandler
import app.softnetwork.notification.message.{
  NotificationAdded,
  NotificationCommandEvent,
  NotificationRemoved
}
import app.softnetwork.persistence.query.{EventProcessorStream, JournalProvider}
import app.softnetwork.notification.message.NotificationCommandEvent

import scala.concurrent.Future

trait NotificationCommandProcessorStream extends EventProcessorStream[NotificationCommandEvent] {
  _: JournalProvider with NotificationHandler =>

  override lazy val tag: String =
    NotificationSettings.NotificationConfig.eventStreams.externalToNotificationTag

  /** @return
    *   whether or not the events processed by this processor stream would be published to the main
    *   bus event
    */
  def forTests: Boolean = false

  /** Processing event
    *
    * @param event
    *   - event to process
    * @param persistenceId
    *   - persistence id
    * @param sequenceNr
    *   - sequence number
    * @return
    */
  override protected def processEvent(
    event: NotificationCommandEvent,
    persistenceId: PersistenceId,
    sequenceNr: Long
  ): Future[Done] = {
    event match {
      case evt: NotificationCommandEvent =>
        evt.command match {
          case Some(command) =>
            !?(command) map {
              case _: NotificationAdded =>
                if (forTests) system.eventStream.tell(Publish(event))
                Done
              case NotificationRemoved =>
                if (forTests) system.eventStream.tell(Publish(event))
                Done
              case other =>
                logger.error(
                  s"$platformEventProcessorId - command $command returns unexpectedly ${other.getClass}"
                )
                Done
            }
          case _ =>
            logger.error(
              s"$platformEventProcessorId - no command"
            )
            Future.successful(Done)
        }
      case other =>
        logger.warn(s"$platformEventProcessorId does not support event [${other.getClass}]")
        Future.successful(Done)
    }
  }
}
