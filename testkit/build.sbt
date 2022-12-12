import app.softnetwork.sbt.build._

Test / parallelExecution := false

organization := "app.softnetwork.notification"

name := "notification-testkit"

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-scheduler-testkit" % Versions.genericPersistence,
  "app.softnetwork.persistence" %% "persistence-session-testkit" % Versions.genericPersistence,
  "com.github.kirviq" % "dumbster" % "1.7.1",
  "org.rapidoid" % "rapidoid-http-server" % "5.5.5"
)
