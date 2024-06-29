package app.softnetwork.notification.config

import configs.Configs

import scala.language.{implicitConversions, reflectiveCalls}

trait NotificationSettings { _: InternalConfig =>
  lazy val NotificationConfig: NotificationConfig =
    Configs[NotificationConfig].get(config, "notification").toEither match {
      case Left(configError) =>
        Console.err.println(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }

}

object NotificationSettings extends NotificationSettings with DefaultConfig
