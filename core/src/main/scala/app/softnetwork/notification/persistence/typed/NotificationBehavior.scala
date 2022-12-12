package app.softnetwork.notification.persistence.typed

import java.util.Date
import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.persistence.typed.scaladsl.Effect
import app.softnetwork.notification.config.NotificationSettings
import org.slf4j.Logger
import org.softnetwork.akka.message.SchedulerEvents.SchedulerEventWithCommand
import app.softnetwork.scheduler.message.{AddSchedule, RemoveSchedule}
import org.softnetwork.akka.model.Schedule
import app.softnetwork.persistence.now
import app.softnetwork.persistence.typed._
import app.softnetwork.notification.message._
import app.softnetwork.notification.model._
import app.softnetwork.notification.spi.NotificationProvider
import org.softnetwork.notification.message._
import org.softnetwork.notification.model._
import app.softnetwork.scheduler.config.{Settings => SchedulerSettings}
import org.softnetwork.notification.model.NotificationStatus._

import scala.language.{implicitConversions, postfixOps}

/** Created by smanciot on 13/04/2020.
  */
trait NotificationBehavior[T <: Notification]
    extends EntityBehavior[NotificationCommand, T, NotificationEvent, NotificationCommandResult]
    with NotificationProvider {

  override type N = T

  /** @return
    *   node role required to start this actor
    */
  override def role: String = NotificationSettings.NotificationConfig.akkaNodeRole

  private[this] val notificationTimerKey: String = "NotificationTimerKey"

  private[this] val delay = 1

  /** Set event tags, which will be used in persistence query
    *
    * @param entityId
    *   - entity id
    * @param event
    *   - the event to tag
    * @return
    *   event tags
    */
  override protected def tagEvent(entityId: String, event: NotificationEvent): Set[String] = {
    event match {
      case _: SchedulerEventWithCommand =>
        Set(
          s"$persistenceId-to-scheduler",
          SchedulerSettings.SchedulerConfig.eventStreams.entityToSchedulerTag
        )
      case _ => Set(persistenceId)
    }
  }

  /** @param entityId
    *   - entity identity
    * @param state
    *   - current state
    * @param command
    *   - command to handle
    * @param replyTo
    *   - optional actor to reply to
    * @return
    *   effect
    */
  override def handleCommand(
    entityId: String,
    state: Option[T],
    command: NotificationCommand,
    replyTo: Option[ActorRef[NotificationCommandResult]],
    timers: TimerScheduler[NotificationCommand]
  )(implicit context: ActorContext[NotificationCommand]): Effect[NotificationEvent, Option[T]] = {
    implicit val log: Logger = context.log
    implicit val system: ActorSystem[Nothing] = context.system

    command match {

      case cmd: AddNotification[T] =>
        import cmd._
        (notification match {
          case n: Mail => Some(MailRecordedEvent(n))
          case n: SMS  => Some(SMSRecordedEvent(n))
          case n: Push => Some(PushRecordedEvent(n))
          case _       => None
        }) match {
          case Some(event) =>
            Effect
              .persist(
                List(
                  event,
                  ScheduleForNotificationAdded(
                    AddSchedule(
                      Schedule(persistenceId, entityId, notificationTimerKey, delay)
                    )
                  )
                )
              )
              .thenRun(_ => NotificationAdded(entityId) ~> replyTo)
          case _ => Effect.none.thenRun(_ => NotificationNotAdded ~> replyTo)
        }

      case _: RemoveNotification =>
        Effect
          .persist(
            List(
              NotificationRemovedEvent(
                entityId
              ),
              ScheduleForNotificationRemoved(
                RemoveSchedule(
                  persistenceId,
                  entityId,
                  notificationTimerKey
                )
              )
            )
          )
          .thenRun(_ => { NotificationRemoved ~> replyTo }) //.thenStop()

      case cmd: SendNotification[T] => sendNotification(entityId, cmd.notification, replyTo)

      case _: ResendNotification =>
        state match {
          case Some(notification) => sendNotification(entityId, notification, replyTo)
          case _                  => Effect.none.thenRun(_ => NotificationNotFound ~> replyTo)
        }

      case _: GetNotificationStatus =>
        state match {
          case Some(notification) =>
            import notification._
            status match {
              case Sent =>
                Effect.none.thenRun(_ =>
                  NotificationSent(entityId, notification.results) ~> replyTo
                )
              case Delivered =>
                Effect.none.thenRun(_ =>
                  NotificationDelivered(entityId, notification.results) ~> replyTo
                )
              case Rejected =>
                Effect.none.thenRun(_ =>
                  NotificationRejected(entityId, notification.results) ~> replyTo
                )
              case Undelivered =>
                Effect.none.thenRun(_ =>
                  NotificationUndelivered(entityId, notification.results) ~> replyTo
                )
              case _ => ackNotification(entityId, notification, replyTo) // Pending
            }
          case _ => Effect.none.thenRun(_ => NotificationNotFound ~> replyTo)
        }

      case cmd: TriggerSchedule4Notification =>
        import cmd.schedule._
        if (key == notificationTimerKey) {
          context.self ! ScheduleNotification
          Effect.none.thenRun(_ => Schedule4NotificationTriggered(cmd.schedule) ~> replyTo)
        } else {
          Effect.none.thenRun(_ => Schedule4NotificationNotTriggered ~> replyTo)
        }

      case ScheduleNotification =>
        state match {
          case Some(notification) => sendNotification(entityId, notification, replyTo)
          case _ => // should never be the case
            Effect
              .persist(
                ScheduleForNotificationRemoved(
                  RemoveSchedule(
                    persistenceId,
                    entityId,
                    notificationTimerKey
                  )
                )
              )
              .thenRun(_ => NotificationNotFound ~> replyTo)
        }

      case _ => super.handleCommand(entityId, state, command, replyTo, timers)
    }
  }

  /** @param state
    *   - current state
    * @param event
    *   - event to hanlde
    * @return
    *   new state
    */
  override def handleEvent(state: Option[T], event: NotificationEvent)(implicit
    context: ActorContext[_]
  ): Option[T] = {
    import context._
    event match {
      case evt: NotificationRecordedEvent[T] =>
        log.info(
          "Recording {}#{} in {} status with {} acknowledgment",
          persistenceId,
          evt.uuid,
          evt.notification.status.name,
          evt.notification.results
        )
        Some(evt.notification)

      case evt: NotificationRemovedEvent =>
        log.info(s"Removing $persistenceId#${evt.uuid}")
        emptyState

      case _ => super.handleEvent(state, event)
    }
  }

  private[this] def scheduledNotificationEvent(entityId: String, notification: T) = {
    import notification._
    if (status.isSent || status.isDelivered) { // the notification has been sent/delivered - the schedule should be removed
      ScheduleForNotificationRemoved(
        RemoveSchedule(
          persistenceId,
          entityId,
          notificationTimerKey
        )
      )
    } else if (
      maxTries > 0 && (nbTries < maxTries || (nbTries == maxTries && (status.isPending || status.isUnknownNotificationStatus)))
    ) {
      ScheduleForNotificationAdded(
        AddSchedule(
          Schedule(persistenceId, entityId, notificationTimerKey, delay)
        )
      )
    } else {
      ScheduleForNotificationRemoved(
        RemoveSchedule(
          persistenceId,
          entityId,
          notificationTimerKey
        )
      )
    }
  }

  private[this] def ackNotification(
    _uuid: String,
    notification: T,
    replyTo: Option[ActorRef[NotificationCommandResult]]
  )(implicit log: Logger, system: ActorSystem[_]): Effect[NotificationEvent, Option[T]] = {
    import notification._
    val notificationAck: NotificationAck = ackUuid match {
      case Some(_) =>
        status match {
          case Pending =>
            log.info(
              "Retrieving acknowledgement for {}#{} in {} status",
              persistenceId,
              _uuid,
              status.name
            )
            ack(
              notification
            ) // we only call the provider api if the notification is pending
          case _ => NotificationAck(ackUuid, results, new Date())
        }
      case _ => NotificationAck(None, results, new Date())
    }
    (notification match {
      case n: Mail =>
        Some(
          MailRecordedEvent(n.copyWithAck(notificationAck).asInstanceOf[Mail])
        )
      case n: SMS =>
        Some(
          SMSRecordedEvent(n.copyWithAck(notificationAck).asInstanceOf[SMS])
        )
      case n: Push =>
        Some(
          PushRecordedEvent(n.copyWithAck(notificationAck).asInstanceOf[Push])
        )
      case _ => None
    }) match {
      case Some(event) =>
        Effect
          .persist(event)
          .thenRun(_ =>
            {
              notificationAck.status match {
                case Rejected    => NotificationRejected(_uuid, notificationAck.results)
                case Undelivered => NotificationUndelivered(_uuid, notificationAck.results)
                case Sent        => NotificationSent(_uuid, notificationAck.results)
                case Delivered   => NotificationDelivered(_uuid, notificationAck.results)
                case _           => NotificationPending(_uuid, notificationAck.results)
              }
            }
            ~> replyTo
          )
      case _ => Effect.unhandled
    }
  }

  private[this] def sendNotification(
    entityId: String,
    notification: T,
    replyTo: Option[ActorRef[NotificationCommandResult]]
  )(implicit
    log: Logger,
    system: ActorSystem[_]
  ): Effect[NotificationEvent, Option[T]] = {
    import notification._
    val maybeAckWithNumberOfRetries: Option[(NotificationAck, Int)] = status match {
      case Sent | Delivered => None
      case Pending | UnknownNotificationStatus =>
        notification.deferred match {
          case Some(deferred) if deferred.after(new Date()) =>
            None // the notification is still deferred
          case _ =>
            if (nbTries > 0) { // the notification has already been sent at least one time, waiting for an acknowledgement
              log.info(
                "Retrieving acknowledgement for {}#{} in {} status",
                persistenceId,
                entityId,
                status.name
              )
              Some(
                (ack(notification), 0)
              ) // FIXME acknowledgment must be properly implemented ...
            } else {
              log.info(
                "Sending {}#{} in {} status to {} recipients",
                persistenceId,
                entityId,
                status.name,
                to.mkString(", ")
              )
              Some((send(notification), 1))
            }
        }
      case _ =>
        // Undelivered or Rejected
        if (maxTries > 0 && nbTries >= maxTries) {
          Some((NotificationAck(notification.ackUuid, notification.results, now()), 1))
        } else {
          log.info(
            "Sending {}#{} in {} status to {} recipients",
            persistenceId,
            entityId,
            status.name,
            to.mkString(", ")
          )
          Some((send(notification), 1))
        }
    }

    val updatedNotification =
      maybeAckWithNumberOfRetries match {
        case Some(ackWithNumberOfRetries) =>
          val notificationAck: NotificationAck = ackWithNumberOfRetries._1
          val nbTries: Int = notification.nbTries + ackWithNumberOfRetries._2
          notification
            .withNbTries(nbTries)
            .copyWithAck(notificationAck)
        case _ => notification
      }

    val event: Option[NotificationEvent] =
      notification match {
        case _: Mail =>
          Some(
            MailRecordedEvent(updatedNotification.asInstanceOf[Mail])
          )
        case _: SMS =>
          Some(
            SMSRecordedEvent(updatedNotification.asInstanceOf[SMS])
          )
        case _: Push =>
          Some(
            PushRecordedEvent(updatedNotification.asInstanceOf[Push])
          )
        case _ => None
      }

    val events: List[NotificationEvent] =
      List(event).flatten :+ scheduledNotificationEvent(
        entityId,
        updatedNotification.asInstanceOf[T]
      )
    Effect
      .persist(events)
      .thenRun(_ =>
        {
          updatedNotification.status match {
            case Rejected    => NotificationRejected(entityId, updatedNotification.results)
            case Undelivered => NotificationUndelivered(entityId, updatedNotification.results)
            case Sent        => NotificationSent(entityId, updatedNotification.results)
            case Delivered   => NotificationDelivered(entityId, updatedNotification.results)
            case _           => NotificationPending(entityId, updatedNotification.results)
          }
        }
        ~> replyTo
      )
  }

}
