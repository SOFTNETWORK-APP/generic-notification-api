package app.softnetwork.notification

import akka.actor.{ActorRef, ActorSystem => ClassicSystem}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.TextMessage
import app.softnetwork.concurrent.Completion
import app.softnetwork.notification.handlers.WsClientsDao
import app.softnetwork.notification.model.{
  Mail,
  Notification,
  NotificationAck,
  NotificationStatus,
  NotificationStatusResult,
  Platform,
  Push,
  PushPayload,
  SMS,
  Ws
}
import app.softnetwork.persistence.typed._

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

package object spi {

  trait NotificationProvider {
    type N <: Notification

    def send(notification: N)(implicit system: ActorSystem[_]): NotificationAck

    def !(notification: N)(implicit system: ActorSystem[_]): NotificationAck = send(notification)

    def ack(notification: N)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())

    def ?(notification: N)(implicit system: ActorSystem[_]): NotificationAck = ack(notification)
  }

  trait MailProvider {
    def sendMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck
    def ackMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  trait SMSProvider {
    def sendSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck
    def ackSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  trait PushProvider {
    def bulkSize = 100
    def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck
    def ackPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(notification.ackUuid, notification.results, Instant.now())
  }

  implicit def toPushPayload(notification: Push): PushPayload = {
    PushPayload.defaultInstance
      .withApplication(
        notification.application.getOrElse(
          notification.from.alias.getOrElse(notification.from.value)
        )
      )
      .withTitle(notification.subject)
      .withBody(notification.message)
      .withBadge(notification.badge)
      .copy(sound = notification.sound)
  }

  trait AndroidProvider extends PushProvider {
    def pushToAndroid(payload: PushPayload, devices: Seq[String])(implicit
      system: ActorSystem[_]
    ): Seq[NotificationStatusResult]
    override def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck = {
      NotificationAck(
        None,
        pushToAndroid(
          notification,
          notification.devices.filter(_.platform == Platform.ANDROID).map(_.regId).distinct
        ).distinct ++
        notification.devices
          .filterNot(_.platform == Platform.ANDROID)
          .map(d =>
            NotificationStatusResult(
              d.regId,
              NotificationStatus.Undelivered,
              Some(s"${d.platform.name} device not handled by AndroidProvider")
            )
          ),
        Instant.now()
      )
    }
  }

  trait IosProvider extends PushProvider {
    def pushToIos(payload: PushPayload, devices: Seq[String])(implicit
      system: ActorSystem[_]
    ): Seq[NotificationStatusResult]
    override def sendPush(notification: Push)(implicit system: ActorSystem[_]): NotificationAck = {
      NotificationAck(
        None,
        pushToIos(
          notification,
          notification.devices.filter(_.platform == Platform.IOS).map(_.regId).distinct
        ).distinct ++
        notification.devices
          .filterNot(_.platform == Platform.IOS)
          .map(d =>
            NotificationStatusResult(
              d.regId,
              NotificationStatus.Undelivered,
              Some(s"${d.platform.name} device not handled by IosProvider")
            )
          ),
        Instant.now()
      )
    }
  }

  trait AndroidAndIosProvider extends PushProvider with AndroidProvider with IosProvider {
    final override def sendPush(
      notification: Push
    )(implicit system: ActorSystem[_]): NotificationAck = {
      // split notification per platform
      val (android, ios) = notification.devices.partition(_.platform == Platform.ANDROID)
      // send notification to devices per platform
      NotificationAck(
        None,
        pushToIos(notification, ios.map(_.regId).distinct) ++ pushToAndroid(
          notification,
          android.map(_.regId)
        ).distinct,
        Instant.now()
      )
    }
  }

  trait WsProvider extends Completion {

    private val wsClientsDao: WsClientsDao = WsClientsDao

    def sendWs(ws: Ws)(implicit system: ActorSystem[_]): NotificationAck = {
      implicit val ec: ExecutionContext = system.executionContext
      val classicSystem: ClassicSystem = system
      val skipped = ws.results
        .filter(result => result.status.isSent || result.status.isDelivered)
        .map(_.recipient)
      val recipients = ws.recipients().filter(recipient => !skipped.contains(recipient))
      val results: Seq[Future[NotificationStatusResult]] = {
        if (recipients.nonEmpty) {
          for (recipient <- recipients) yield {
            WsClients.lookup(recipient) match {
              case Some(actorRef) =>
                actorRef ! TextMessage.Strict(ws.message)
                Future.successful(
                  NotificationStatusResult.defaultInstance
                    .withUuid(ws.uuid)
                    .withRecipient(recipient)
                    .withStatus(NotificationStatus.Sent)
                )
              case None =>
                wsClientsDao.lookupKeyValue(recipient) flatMap {
                  case Some(ref) =>
                    classicSystem
                      .actorSelection(ref)
                      .resolveOne(FiniteDuration(5, "seconds"))
                      .map(actorRef => {
                        actorRef ! TextMessage.Strict(ws.message)
                        NotificationStatusResult.defaultInstance
                          .withUuid(ws.uuid)
                          .withRecipient(recipient)
                          .withStatus(NotificationStatus.Sent)
                      })
                      .recover { case e: Throwable =>
                        wsClientsDao.removeKeyValue(recipient)
                        Console.err.println(
                          s"Error while sending notification to client $recipient: ${e.getMessage}"
                        )
                        NotificationStatusResult.defaultInstance
                          .withUuid(ws.uuid)
                          .withRecipient(recipient)
                          .withStatus(NotificationStatus.Rejected)
                          .withError(e.getMessage)
                      }
                  case None =>
                    val error = s"ActorRef for client $recipient not found"
                    Console.err.println(error)
                    Future.successful(
                      NotificationStatusResult.defaultInstance
                        .withUuid(ws.uuid)
                        .withRecipient(recipient)
                        .withStatus(NotificationStatus.Rejected)
                        .withError(error)
                    )
                }
            }
          }
        } else {
          val error = s"No recipient found for notification ${ws.uuid}"
          Console.err.println(error)
          Seq(
            Future.successful(
              NotificationStatusResult.defaultInstance
                .withUuid(ws.uuid)
                .withRecipient("__unknown__")
                .withStatus(NotificationStatus.Rejected)
                .withError(error)
            )
          )
        }
      }
      Future.sequence(results).flatMap(results => Future.successful(results)) complete () match {
        case Success(s) =>
          val existingResults =
            ws.results.filterNot(result => s.exists(_.recipient == result.recipient))
          ackClient(ws.withResults(existingResults ++ s))
        case Failure(f) =>
          Console.err.println(f.getMessage)
          ackClient(ws)
      }
    }

    def ackClient(notification: Ws)(implicit system: ActorSystem[_]): NotificationAck =
      NotificationAck(Some(s"${notification.uuid}-ack"), notification.results, Instant.now())
  }

  object WsClients {
    private[this] var clients: Map[String, ActorRef] = Map.empty
    def add(client: String, actorRef: ActorRef): Unit = clients += client -> actorRef
    def remove(client: String): Unit = clients -= client
    def lookup(client: String): Option[ActorRef] = clients.get(client)
  }

  object WsChannels {
    private[this] var channels: Map[String, Set[String]] = Map.empty
    def addSession(channel: String, session: String): Unit = {
      Console.out.println(s"Adding session $session to channel $channel")
      channels += channel -> channels.get(channel).map(_ + session).getOrElse(Set(session))
    }
    def removeSession(channel: String, session: String): Unit = {
      Console.out.println(s"Removing session $session from channel $channel")
      channels += channel -> channels.get(channel).map(_ - session).getOrElse(Set.empty)
    }
    def lookupClients(channel: String): Option[Set[String]] = channels
      .get(channel)
      .map(sessions => sessions.flatMap(session => WsSessions.lookupClients(session)).flatten)
  }

  object WsSessions {
    private[this] var sessions: Map[String, Set[String]] = Map.empty
    def addClient(session: String, client: String): Unit =
      sessions += session -> sessions.get(session).map(_ + client).getOrElse(Set(client))
    def removeClient(session: String, client: String): Unit =
      sessions += session -> sessions.get(session).map(_ - client).getOrElse(Set.empty)
    def lookupClients(session: String): Option[Set[String]] = sessions.get(session)
  }

  trait MailAndSMSAndFcmAndIosAndWsProvider
      extends NotificationProvider
      with MailProvider
      with SMSProvider
      with AndroidAndIosProvider
      with WsProvider {
    override type N = Notification

    override def send(
      notification: Notification
    )(implicit system: ActorSystem[_]): NotificationAck = notification match {
      case mail: Mail => sendMail(mail)
      case sms: SMS   => sendSMS(sms)
      case push: Push => sendPush(push)
      case ws: Ws     => sendWs(ws)
      case _          => NotificationAck(notification.ackUuid, notification.results, Instant.now())
    }

    override def ack(notification: Notification)(implicit system: ActorSystem[_]): NotificationAck =
      notification match {
        case mail: Mail => ackMail(mail)
        case sms: SMS   => ackSMS(sms)
        case push: Push => ackPush(push)
        case ws: Ws     => ackClient(ws)
        case _ => NotificationAck(notification.ackUuid, notification.results, Instant.now())
      }
  }

}
