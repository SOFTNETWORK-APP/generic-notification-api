package app.softnetwork.notification.config

case class ApnsConfig(topic: String, keystore: Keystore, dryRun: Boolean = false)
