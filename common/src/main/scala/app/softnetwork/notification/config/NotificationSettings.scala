package app.softnetwork.notification.config

import com.typesafe.scalalogging.StrictLogging
import configs.Configs

import scala.language.{implicitConversions, reflectiveCalls}

trait NotificationSettings extends StrictLogging { _: InternalConfig =>
  lazy val NotificationConfig: NotificationConfig =
    Configs[NotificationConfig].get(config, "notification").toEither match {
      case Left(configError) =>
        logger.error(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }

}

object NotificationSettings extends NotificationSettings with DefaultConfig
