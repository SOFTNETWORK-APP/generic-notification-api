package app.softnetwork.notification.spi

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import app.softnetwork.api.server.DefaultComplete
import app.softnetwork.notification.config.{SMSMode, SMSSettings}
import app.softnetwork.notification.serialization.notificationFormats
import app.softnetwork.persistence.generateUUID
import org.json4s.Formats

object SMSModeService extends Directives with DefaultComplete {

  implicit def formats: Formats = notificationFormats

  lazy val config: Option[SMSMode.Config] = SMSSettings.SMSConfig.mode

  lazy val version: String = config.map(_.version).orNull

  lazy val route: Route = {
    path(version) {
      path("sendSMS.do") {
        get {
          complete(StatusCodes.OK, s"ACCEPTED | DESCRIPTION | ${generateUUID()}")
        }
      } ~
      path("compteRendu.do") {
        get {
          complete(StatusCodes.OK, s"SENT")
        }
      }
    }

  }

}
