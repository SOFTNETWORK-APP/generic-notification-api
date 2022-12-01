package app.softnetwork.notification.config

import configs.Configs

trait MailSettings extends NotificationSettings {

  lazy val MailConfig: MailConfig =
    Configs[MailConfig].get(config, "notification.mail").toEither match {
      case Left(configError) =>
        logger.error(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }
}

object MailSettings extends MailSettings