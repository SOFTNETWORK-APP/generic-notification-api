package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.testkit.WSProbe
import app.softnetwork.api.server.config.ServerSettings
import app.softnetwork.notification.config.NotificationSettings
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.service.NotificationService
import app.softnetwork.notification.spi.{WsChannels, WsSessions}
import app.softnetwork.session.model.{SessionData, SessionDataCompanion, SessionDataDecorator}
import app.softnetwork.session.service.SessionMaterials
import com.softwaremill.session.{RefreshTokenStorage, SessionConfig, SessionManager}
import org.scalatest.Suite
import org.softnetwork.session.model.Session

import java.net.URLEncoder
import scala.concurrent.ExecutionContext

trait AllNotificationsRoutesTestKit[SD <: SessionData with SessionDataDecorator[SD]]
    extends NotificationRoutesTestKit[SD, Notification]
    with AllNotificationsApiRoutesTestKit[SD]
    with WsClientTestKit { self: Suite with SessionMaterials[SD] =>

  override def notificationService: ActorSystem[_] => NotificationService[SD] = sys =>
    new NotificationService[SD] with SessionMaterials[SD] {
      override implicit def system: ActorSystem[_] = sys

      override lazy val ec: ExecutionContext = sys.executionContext

      override protected def sessionType: Session.SessionType = self.sessionType

      override implicit def manager(implicit
        sessionConfig: SessionConfig,
        companion: SessionDataCompanion[SD]
      ): SessionManager[SD] = self.manager

      override implicit def refreshTokenStorage: RefreshTokenStorage[SD] =
        self.refreshTokenStorage

      override implicit def companion: SessionDataCompanion[SD] = self.companion
    }

  val wsPath = s"/${ServerSettings.RootPath}/${NotificationSettings.NotificationConfig.path}"

  override def ws(
    clientId: String,
    sessionId: String,
    channel: Option[String] = None
  ): Option[WSProbe] = {
    val encodedClientId = URLEncoder.encode(clientId, "UTF-8")
    channel match {
      case Some(c) => addChannel(c)
      case _       =>
    }
    val wsClient: WSProbe = WSProbe()
    withHeaders(
      WS(s"$wsPath/connect/$encodedClientId", wsClient.flow)
    ) ~> routes ~> check {
      isWebSocketUpgrade shouldEqual true
      WsSessions.lookupClients(sessionId).getOrElse(Set.empty).contains(clientId) shouldEqual true
      channel match {
        case Some(c) =>
          WsChannels.lookupClients(c).getOrElse(Set.empty).contains(clientId) shouldEqual true
        case _ =>
      }
      Some(wsClient)
    }
  }

  override def addChannel(channel: String): Unit = {
    withHeaders(
      Post(s"$wsPath/channels/${URLEncoder.encode(channel, "UTF-8")}")
    ) ~> routes ~> check {
      status.isSuccess() shouldEqual true
      /*httpHeaders = extractHeaders(headers)
      extractSession(false) match {
        case None => fail()
        case Some(session) =>
          session
            .get("channels")
            .getOrElse("")
            .split(",")
            .filter(_.nonEmpty)
            .toSet
            .contains(channel) shouldEqual true
      }*/
    }
  }

  override def removeChannel(channel: String): Unit = {
    withHeaders(
      Delete(s"$wsPath/channels/${URLEncoder.encode(channel, "UTF-8")}")
    ) ~> routes ~> check {
      status.isSuccess() shouldEqual true
      /*httpHeaders = extractHeaders(headers)
      extractSession(false) match {
        case None => fail()
        case Some(session) =>
          session
            .get("channels")
            .getOrElse("")
            .split(",")
            .filter(_.nonEmpty)
            .toSet
            .contains(channel) shouldEqual false
      }*/
    }
  }
}
