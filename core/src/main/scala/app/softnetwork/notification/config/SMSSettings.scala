package app.softnetwork.notification.config

import configs.Configs

import scala.language.reflectiveCalls

trait SMSSettings extends NotificationSettings { _: InternalConfig =>

  lazy val SMSConfig: SMSConfig =
    Configs[SMSConfig].get(config, "notification.sms").toEither match {
      case Left(configError) =>
        Console.err.println(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }
}

object SMSSettings extends SMSSettings with DefaultConfig
