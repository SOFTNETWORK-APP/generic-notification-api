package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.MockNotificationHandler

trait MockNotificationServer extends NotificationServer with MockNotificationHandler

object MockNotificationServer {
  def apply(sys: ActorSystem[_]): MockNotificationServer = {
    new MockNotificationServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
