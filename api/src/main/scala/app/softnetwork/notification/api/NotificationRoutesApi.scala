package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.launch.NotificationRoutes
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.service.NotificationService
import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.session.CsrfCheckHeader
import app.softnetwork.session.model.{SessionData, SessionDataCompanion, SessionDataDecorator}
import app.softnetwork.session.service.SessionMaterials
import com.softwaremill.session.{RefreshTokenStorage, SessionConfig, SessionManager}
import org.softnetwork.session.model.Session

import scala.concurrent.ExecutionContext

trait NotificationRoutesApi[SD <: SessionData with SessionDataDecorator[SD], T <: Notification]
    extends NotificationRoutes[SD, T]
    with CsrfCheckHeader {
  self: NotificationApi[SD, T] with SchemaProvider =>
  override def notificationService: ActorSystem[_] => NotificationService[SD] = sys =>
    new NotificationService[SD] with SessionMaterials[SD] {
      override implicit def manager(implicit
        sessionConfig: SessionConfig,
        companion: SessionDataCompanion[SD]
      ): SessionManager[SD] = self.manager

      override protected def sessionType: Session.SessionType = self.sessionType

      override implicit def sessionConfig: SessionConfig = self.sessionConfig

      override implicit def system: ActorSystem[_] = sys

      override lazy val ec: ExecutionContext = sys.executionContext

      override implicit def refreshTokenStorage: RefreshTokenStorage[SD] =
        self.refreshTokenStorage(sys)

      override implicit def companion: SessionDataCompanion[SD] = self.companion
    }
}
