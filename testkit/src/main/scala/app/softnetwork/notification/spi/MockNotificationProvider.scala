package app.softnetwork.notification.spi

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.model.Notification
import com.typesafe.scalalogging.StrictLogging
import org.softnetwork.notification.model.{
  Mail,
  NotificationAck,
  NotificationStatus,
  NotificationStatusResult
}

import java.util.{Date, UUID}

trait MockNotificationProvider extends NotificationProvider with StrictLogging {

  override type N = Notification

  override def send(
    notification: Notification
  )(implicit system: ActorSystem[_]): NotificationAck = {
    notification match {
      case m: Mail => logger.info(s"\r\n${m.richMessage}")
      case _       => logger.info(s"\r\n${notification.message}")
    }
    NotificationAck(
      Some(UUID.randomUUID().toString),
      notification.to.map(recipient =>
        NotificationStatusResult(recipient, NotificationStatus.Sent, None)
      ),
      new Date()
    )
  }

}
