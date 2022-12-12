package app.softnetwork.notification.config

import com.typesafe.config.{Config, ConfigFactory}

trait InternalConfig {
  def config: Config
}

trait DefaultConfig extends InternalConfig {
  lazy val config: Config = ConfigFactory.load()
}
