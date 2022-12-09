package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.FcmMockNotificationsHandler

trait FcmNotificationsServer extends NotificationServer with FcmMockNotificationsHandler

object FcmNotificationsServer {
  def apply(sys: ActorSystem[_]): FcmNotificationsServer = {
    new FcmNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
