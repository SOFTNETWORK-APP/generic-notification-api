package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.FcmAndApnsNotificationsHandler

trait FcmAndApnsNotificationsServer extends NotificationServer with FcmAndApnsNotificationsHandler

object FcmAndApnsNotificationsServer {
  def apply(sys: ActorSystem[_]): FcmAndApnsNotificationsServer = {
    new FcmAndApnsNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
