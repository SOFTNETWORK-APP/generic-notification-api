import app.softnetwork.sbt.build._

Test / parallelExecution := false

organization := "app.softnetwork.notification"

name := "notification-testkit"

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-scheduler-testkit" % Versions.genericPersistence,
  "app.softnetwork.persistence" %% "persistence-session-testkit" % Versions.genericPersistence,
  "com.github.kirviq" % "dumbster" % "1.7.1"
)

Test / envVars := Map(
  "NOTIFICATION_PUSH_MOCK_APNS_HOSTNAME" -> Utils.hostname,
  "NOTIFICATION_PUSH_MOCK_APNS_PORT" -> Utils.availablePort.toString,
  "NOTIFICATION_MAIL_HOST" -> Utils.hostname,
  "NOTIFICATION_MAIL_PORT" -> Utils.availablePort.toString
)
