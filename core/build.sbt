import app.softnetwork.sbt.build._

organization := "app.softnetwork.notification"

name := "notification-core"

libraryDependencies ++= Seq(
  "app.softnetwork.scheduler" %% "scheduler-core" % Versions.scheduler,
  "app.softnetwork.persistence" %% "persistence-session" % Versions.genericPersistence
)
