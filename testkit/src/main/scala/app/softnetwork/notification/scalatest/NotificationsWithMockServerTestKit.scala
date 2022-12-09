package app.softnetwork.notification.scalatest

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.NotificationSettings.NotificationConfig
import app.softnetwork.notification.model.Notification
import app.softnetwork.notification.spi.NotificationMockServer
import org.scalatest.Suite

trait NotificationsWithMockServerTestKit[T <: Notification] extends NotificationTestKit[T] {
  _: Suite with NotificationMockServer =>
  override implicit lazy val system: ActorSystem[_] = typedSystem()

  /** @return
    *   roles associated with this node
    */
  override def roles: Seq[String] = super.roles :+ NotificationConfig.akkaNodeRole

  override def beforeAll(): Unit = {
    super.beforeAll()
    assert(initMockServer())
  }
}
