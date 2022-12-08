package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.ApnsNotificationHandler

trait ApnsNotificationServer extends NotificationServer with ApnsNotificationHandler

object ApnsNotificationServer {
  def apply(sys: ActorSystem[_]): ApnsNotificationServer = {
    new ApnsNotificationServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
