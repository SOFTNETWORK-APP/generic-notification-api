import app.softnetwork.sbt.build._

Test / parallelExecution := false

organization := "app.softnetwork.notification"

name := "notification-testkit"

libraryDependencies ++= Seq(
  "app.softnetwork.scheduler" %% "scheduler-testkit" % Versions.scheduler,
  "app.softnetwork.api" %% "generic-server-api-testkit" % Versions.server,
  "com.github.kirviq" % "dumbster" % "1.7.1",
  "org.rapidoid" % "rapidoid-http-server" % "5.5.5"
)
