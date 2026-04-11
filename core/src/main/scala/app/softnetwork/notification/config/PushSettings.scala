package app.softnetwork.notification.config

import configs.ConfigReader

import scala.jdk.CollectionConverters._
import scala.language.{implicitConversions, reflectiveCalls}

trait PushSettings extends NotificationSettings { _: InternalConfig =>

  lazy val DefaultConfig: PushConfig =
    ConfigReader[PushConfig].read(config, "notification.push").toEither match {
      case Left(configError) =>
        Console.err.println(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }

  lazy val AppConfigs: Map[String, PushConfig] = config
    .getStringList("notification.push.apps")
    .asScala
    .toList
    .map(app =>
      app -> (ConfigReader[PushConfig].read(config, s"notification.push.$app").toEither match {
        case Left(configError) =>
          Console.err.println(s"Something went wrong with the provided arguments $configError")
          throw configError.configException
        case Right(r) => r
      })
    )
    .toMap
}

object PushSettings extends PushSettings with DefaultConfig
