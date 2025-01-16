organization := "app.softnetwork.notification"

name := "notification-core"

libraryDependencies ++= Seq(
  "app.softnetwork.session" %% "session-core" % Versions.genericPersistence
)