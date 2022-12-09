package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.MockAllNotificationsHandler

trait MockAllNotificationsServer extends AllNotificationsServer with MockAllNotificationsHandler

object MockAllNotificationsServer {
  def apply(sys: ActorSystem[_]): MockAllNotificationsServer = {
    new MockAllNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
