package app.softnetwork.notification.config

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import configs.Configs

import scala.language.implicitConversions

trait NotificationSettings extends StrictLogging {

  lazy val config: Config = ConfigFactory.load()

  lazy val NotificationConfig: NotificationConfig =
    Configs[NotificationConfig].get(config, "notification").toEither match {
      case Left(configError) =>
        logger.error(s"Something went wrong with the provided arguments $configError")
        throw configError.configException
      case Right(r) => r
    }

}

object NotificationSettings extends NotificationSettings
