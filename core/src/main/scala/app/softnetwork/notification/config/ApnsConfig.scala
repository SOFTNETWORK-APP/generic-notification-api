package app.softnetwork.notification.config

case class ApnsConfig(
  topic: String,
  keystore: Keystore,
  dryRun: Boolean = false,
  hostname: Option[String] = None,
  port: Option[Int] = None,
  truststore: Option[String] = None
)
