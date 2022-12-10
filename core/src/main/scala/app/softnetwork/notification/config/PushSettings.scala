package app.softnetwork.notification.config

import configs.Configs

import scala.collection.JavaConverters._
import scala.language.{implicitConversions, reflectiveCalls}

trait PushSettings extends NotificationSettings { _: InternalConfig =>

  lazy val DefaultConfig: PushConfig =
    Configs[PushConfig].get(config, "notification.push").toEither match {
      case Left(configError) =>
        logger.error(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }

  lazy val AppConfigs: Map[String, PushConfig] = config
    .getStringList("notification.push.apps")
    .asScala
    .toList
    .map(app => app -> Configs[PushConfig].get(config, s"notification.push.$app").value)
    .toMap
}

object PushSettings extends PushSettings with DefaultConfig
