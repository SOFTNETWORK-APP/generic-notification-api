package app.softnetwork.notification.config

case class MailConfig(
  host: String,
  port: Int,
  sslPort: Int,
  username: String,
  password: String,
  sslEnabled: Boolean,
  sslCheckServerIdentity: Boolean,
  startTLSEnabled: Boolean,
  socketConnectionTimeout: Int = 2000,
  socketTimeout: Int = 2000
)
