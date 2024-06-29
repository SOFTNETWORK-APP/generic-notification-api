package app.softnetwork.notification.config

import configs.Configs

import scala.language.reflectiveCalls

trait MailSettings extends NotificationSettings { _: InternalConfig =>

  lazy val MailConfig: MailConfig =
    Configs[MailConfig].get(config, "notification.mail").toEither match {
      case Left(configError) =>
        Console.err.println(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }
}

object MailSettings extends MailSettings with DefaultConfig
