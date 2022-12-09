package app.softnetwork.notification.scalatest

import app.softnetwork.notification.config.NotificationSettings.NotificationConfig
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification
import app.softnetwork.scheduler.scalatest.SchedulerTestKit
import org.scalatest.Suite

trait NotificationTestKit[T <: Notification] extends SchedulerTestKit with NotificationGuardian[T] {
  _: Suite =>

  /** @return
    *   roles associated with this node
    */
  override def roles: Seq[String] = super.roles :+ NotificationConfig.akkaNodeRole

}
