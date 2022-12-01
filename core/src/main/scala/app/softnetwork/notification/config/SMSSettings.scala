package app.softnetwork.notification.config

import configs.Configs

trait SMSSettings extends NotificationSettings {

  lazy val SMSConfig: SMSConfig =
    Configs[SMSConfig].get(config, "notification.sms").toEither match {
      case Left(configError) =>
        logger.error(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }
}

object SMSSettings extends SMSSettings