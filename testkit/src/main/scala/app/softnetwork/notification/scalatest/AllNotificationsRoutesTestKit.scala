package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.service.NotificationService
import app.softnetwork.session.model.{SessionData, SessionDataCompanion, SessionDataDecorator}
import app.softnetwork.session.service.SessionMaterials
import com.softwaremill.session.{RefreshTokenStorage, SessionConfig, SessionManager}
import org.scalatest.Suite
import org.slf4j.{Logger, LoggerFactory}
import org.softnetwork.session.model.Session

import scala.concurrent.ExecutionContext

trait AllNotificationsRoutesTestKit[SD <: SessionData with SessionDataDecorator[SD]]
    extends NotificationRoutesTestKit[SD, Notification]
    with AllNotificationsApiRoutesTestKit[SD] { self: Suite with SessionMaterials[SD] =>

  override def notificationService: ActorSystem[_] => NotificationService[SD] = sys =>
    new NotificationService[SD] with SessionMaterials[SD] {
      override implicit def system: ActorSystem[_] = sys

      override lazy val ec: ExecutionContext = sys.executionContext
      lazy val log: Logger = LoggerFactory getLogger getClass.getName

      override protected def sessionType: Session.SessionType = self.sessionType

      override implicit def manager(implicit
        sessionConfig: SessionConfig,
        companion: SessionDataCompanion[SD]
      ): SessionManager[SD] = self.manager

      override implicit def refreshTokenStorage: RefreshTokenStorage[SD] =
        self.refreshTokenStorage

      override implicit def companion: SessionDataCompanion[SD] = self.companion
    }
}
