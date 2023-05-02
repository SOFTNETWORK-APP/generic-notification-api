package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.ApnsNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait ApnsNotificationsServer extends NotificationServer with ApnsNotificationsHandler

object ApnsNotificationsServer {
  def apply(sys: ActorSystem[_]): ApnsNotificationsServer = {
    new ApnsNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
