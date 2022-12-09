package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.AllNotificationsHandler

trait AllNotificationsServer extends NotificationServer with AllNotificationsHandler

object AllNotificationsServer {
  def apply(sys: ActorSystem[_]): AllNotificationsServer = {
    new AllNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
