package app.softnetwork.notification.scalatest

import akka.http.scaladsl.testkit.InMemoryPersistenceScalatestRouteTest
import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.api.server.scalatest.ServerTestKit
import app.softnetwork.notification.api.NotificationGrpcServicesTestKit
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.model.Notification
import org.scalatest.Suite

trait NotificationApiRoutesTestKit[T <: Notification]
    extends NotificationTestKit[T]
    with NotificationGrpcServicesTestKit[T]
    with InMemoryPersistenceScalatestRouteTest {
  _: Suite with NotificationGuardian[T] with ApiRoutes with ServerTestKit =>

}
