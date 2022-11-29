import app.softnetwork.sbt.build._

organization := "app.softnetwork.notification"

name := "notification-core"

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-session" % Versions.genericPersistence
)
