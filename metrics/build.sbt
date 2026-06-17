organization := "app.softnetwork.notification"

name := "notification-metrics"

libraryDependencies ++= Seq(
  // Lean, dependency-light module (Story 13.9): only the Prometheus client, so consumers
  // (e.g. softclient4es-license-server) can depend on notification metrics WITHOUT pulling the
  // full notification-common stack (firebase-admin, pushy, commons-email, ...).
  "io.prometheus" % "prometheus-metrics-core" % Versions.prometheus
)
