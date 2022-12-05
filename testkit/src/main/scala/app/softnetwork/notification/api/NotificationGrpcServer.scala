package app.softnetwork.notification.api

import akka.http.scaladsl.testkit.PersistenceScalatestGrpcTest
import app.softnetwork.persistence.scalatest.InMemoryPersistenceTestKit
import org.scalatest.Suite

trait NotificationGrpcServer
    extends PersistenceScalatestGrpcTest
    with NotificationGrpcServices
    with InMemoryPersistenceTestKit { _: Suite =>
  override lazy val additionalConfig: String = grpcConfig
}
