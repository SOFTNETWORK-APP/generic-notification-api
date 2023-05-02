package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.SimpleMailNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait SimpleMailNotificationsServer extends NotificationServer with SimpleMailNotificationsHandler

object SimpleMailNotificationsServer {
  def apply(sys: ActorSystem[_]): SimpleMailNotificationsServer = {
    new SimpleMailNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
