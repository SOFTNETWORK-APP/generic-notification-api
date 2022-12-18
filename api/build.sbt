import app.softnetwork.sbt.build._
import com.typesafe.sbt.packager.docker._

Compile / mainClass := Some("app.softnetwork.notification.api.AllNotificationsPostgresLauncher")

dockerBaseImage := "openjdk:8"

dockerEntrypoint := Seq(s"${(Docker / defaultLinuxInstallLocation).value}/bin/entrypoint.sh")

dockerExposedVolumes := Seq(
  s"${(Docker / defaultLinuxInstallLocation).value}/conf",
  s"${(Docker / defaultLinuxInstallLocation).value}/logs"
)

dockerExposedPorts := Seq(
  9000,
  5000,
  8558,
  25520
)

dockerRepository := Some("softnetwork.jfrog.io/default-docker-local")

bashScriptDefines / scriptClasspath ~= (cp => "../conf" +: cp)

organization := "app.softnetwork.notification"

name := "notification-api"

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-jdbc" % Versions.genericPersistence,
  "app.softnetwork.scheduler" %% "scheduler-api" % Versions.scheduler
)
