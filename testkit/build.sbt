import app.softnetwork.sbt.build._

Test / parallelExecution := false

organization := "app.softnetwork.notification"

name := "notification-testkit"

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-scheduler-testkit" % Versions.genericPersistence,
  "app.softnetwork.persistence" %% "persistence-session-testkit" % Versions.genericPersistence
)

Test / envVars := Map(
  "NOTIFICATION_PUSH_MOCK_APNS_HOSTNAME" -> Utils.hostname,
  "NOTIFICATION_PUSH_MOCK_APNS_PORT" -> Utils.availablePort.toString
)
