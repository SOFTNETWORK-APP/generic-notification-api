organization := "app.softnetwork.notification"

name := "notification-common"

val guavaExclusion =  ExclusionRule(organization = "com.google.guava", name="guava")

libraryDependencies ++= Seq(
  "app.softnetwork.persistence" %% "persistence-kv" % Versions.genericPersistence,
  "app.softnetwork.scheduler" %% "scheduler-common" % Versions.scheduler,
  "app.softnetwork.scheduler" %% "scheduler-common" % Versions.scheduler % "protobuf",
  "app.softnetwork.api" %% "generic-server-api" % Versions.genericPersistence,
  "app.softnetwork.protobuf" %% "scalapb-extensions" % "0.1.7",
  "org.apache.commons" % "commons-email" % "1.5",
  "com.google.auth" % "google-auth-library-oauth2-http" % "0.20.0" excludeAll guavaExclusion,
  "io.opencensus" % "opencensus-contrib-http-util" % "0.24.0" excludeAll(guavaExclusion, ExclusionRule(organization = "io.grpc", name="grpc-context")),
  "com.google.firebase" % "firebase-admin" % "9.3.0" excludeAll(
    ExclusionRule(organization = "io.opencensus", name="opencensus-contrib-http-util"),
    ExclusionRule(organization = "com.google.auth", name = "google-auth-library-oauth2-http")
    /*, ExclusionRule(organization = "org.apache.commons", name = "commons-lang3")*/,
    ExclusionRule(organization = "io.netty"),
    ExclusionRule(organization = "io.grpc"),
    ExclusionRule(organization = "org.slf4j"),
  ),
  "com.eatthepath" % "pushy" % "0.15.1"
)

Compile / unmanagedResourceDirectories += baseDirectory.value / "src/main/protobuf"
