package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.AllNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait AllNotificationsServer extends NotificationServer with AllNotificationsHandler

object AllNotificationsServer {
  def apply(sys: ActorSystem[_]): AllNotificationsServer = {
    new AllNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
