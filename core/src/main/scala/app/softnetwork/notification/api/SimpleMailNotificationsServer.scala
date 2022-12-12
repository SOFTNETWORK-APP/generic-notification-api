package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.SimpleMailNotificationsHandler

trait SimpleMailNotificationsServer extends NotificationServer with SimpleMailNotificationsHandler

object SimpleMailNotificationsServer {
  def apply(sys: ActorSystem[_]): SimpleMailNotificationsServer = {
    new SimpleMailNotificationsServer {
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
