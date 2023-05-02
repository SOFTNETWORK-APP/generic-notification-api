package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.handlers.FcmAndApnsNotificationsHandler
import org.slf4j.{Logger, LoggerFactory}

trait FcmAndApnsNotificationsServer extends NotificationServer with FcmAndApnsNotificationsHandler

object FcmAndApnsNotificationsServer {
  def apply(sys: ActorSystem[_]): FcmAndApnsNotificationsServer = {
    new FcmAndApnsNotificationsServer {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName
      override implicit val system: ActorSystem[_] = sys
    }
  }
}
