package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.SMSModeNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait SMSModeNotificationsServer extends NotificationServer with SMSModeNotificationsHandler

object SMSModeNotificationsServer {
  def apply(sys: ActorSystem[_]): SMSModeNotificationsServer = {
    new SMSModeNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
