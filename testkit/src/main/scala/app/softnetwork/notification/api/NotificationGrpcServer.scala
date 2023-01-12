package app.softnetwork.notification.api

import akka.http.scaladsl.testkit.PersistenceScalatestGrpcTest
import app.softnetwork.api.server.scalatest.ServerTestKit
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification
import app.softnetwork.persistence.scalatest.InMemoryPersistenceTestKit
import app.softnetwork.scheduler.launch.SchedulerGuardian
import org.scalatest.Suite

trait NotificationGrpcServer[T <: Notification]
    extends PersistenceScalatestGrpcTest
    with NotificationGrpcServices[T]
    with InMemoryPersistenceTestKit {
  _: Suite with NotificationGuardian[T] with SchedulerGuardian with ServerTestKit =>
  override lazy val additionalConfig: String = notificationGrpcConfig
}
