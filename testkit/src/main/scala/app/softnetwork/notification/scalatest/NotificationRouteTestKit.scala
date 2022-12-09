package app.softnetwork.notification.scalatest

import akka.http.scaladsl.testkit.InMemoryPersistenceScalatestRouteTest
import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.api.NotificationGrpcServices
import app.softnetwork.notification.model.Notification
import org.scalatest.Suite

trait NotificationRouteTestKit[T <: Notification]
    extends InMemoryPersistenceScalatestRouteTest
    with ApiRoutes
    with NotificationTestKit[T]
    with NotificationGrpcServices[T] { _: Suite =>

  override lazy val additionalConfig: String = grpcConfig

}
