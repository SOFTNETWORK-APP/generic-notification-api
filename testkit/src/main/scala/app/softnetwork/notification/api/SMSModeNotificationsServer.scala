package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.SMSModeNotificationsHandler

trait SMSModeNotificationsServer extends NotificationServer with SMSModeNotificationsHandler

object SMSModeNotificationsServer {
  def apply(sys: ActorSystem[_]): SMSModeNotificationsServer = {
    new SMSModeNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
