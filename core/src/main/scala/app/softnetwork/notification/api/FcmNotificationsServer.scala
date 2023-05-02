package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.FcmNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait FcmNotificationsServer extends NotificationServer with FcmNotificationsHandler

object FcmNotificationsServer {
  def apply(sys: ActorSystem[_]): FcmNotificationsServer = {
    new FcmNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
