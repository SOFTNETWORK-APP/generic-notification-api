package app.softnetwork.notification.config

import configs.ConfigReader

import scala.language.reflectiveCalls

trait SMSSettings extends NotificationSettings { _: InternalConfig =>

  lazy val SMSConfig: SMSConfig =
    ConfigReader[SMSConfig].read(config, "notification.sms").toEither match {
      case Left(configError) =>
        Console.err.println(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }
}

object SMSSettings extends SMSSettings with DefaultConfig
