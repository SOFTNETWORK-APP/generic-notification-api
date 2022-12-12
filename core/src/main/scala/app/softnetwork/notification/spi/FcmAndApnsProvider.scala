package app.softnetwork.notification.spi

import app.softnetwork.notification.config.InternalConfig

trait FcmAndApnsProvider extends AndroidAndIosProvider with FcmProvider with ApnsProvider {
  _: InternalConfig =>
}
