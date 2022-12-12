import app.softnetwork.sbt.build._

organization := "app.softnetwork.notification"

name := "notification-common"

val guavaExclusion =  ExclusionRule(organization = "com.google.guava", name="guava")

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-kv" % Versions.genericPersistence,
  "app.softnetwork.persistence" %% "persistence-scheduler" % Versions.genericPersistence,
  "app.softnetwork.persistence" %% "persistence-scheduler" % Versions.genericPersistence % "protobuf",
  "app.softnetwork.api" %% "generic-server-api" % Versions.genericPersistence,
  "app.softnetwork.protobuf" %% "scalapb-extensions" % "0.1.5",
  "org.apache.commons" % "commons-email" % "1.5",
  "com.google.auth" % "google-auth-library-oauth2-http" % "0.20.0" excludeAll guavaExclusion,
  "io.opencensus" % "opencensus-contrib-http-util" % "0.24.0" excludeAll(guavaExclusion, ExclusionRule(organization = "io.grpc", name="grpc-context")),
  "com.google.firebase" % "firebase-admin" % "7.1.0" excludeAll(ExclusionRule(organization = "io.opencensus", name="opencensus-contrib-http-util"), ExclusionRule(organization = "com.google.auth", name = "google-auth-library-oauth2-http")/*, ExclusionRule(organization = "org.apache.commons", name = "commons-lang3")*/, ExclusionRule(organization = "io.netty")),
  "com.eatthepath" % "pushy" % "0.15.1"
)

Compile / unmanagedResourceDirectories += baseDirectory.value / "src/main/protobuf"
