package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.ApnsNotificationsHandler

trait ApnsNotificationsServer extends NotificationServer with ApnsNotificationsHandler

object ApnsNotificationsServer {
  def apply(sys: ActorSystem[_]): ApnsNotificationsServer = {
    new ApnsNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
