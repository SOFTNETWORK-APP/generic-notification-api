package app.softnetwork.notification.spi

import app.softnetwork.notification.config.InternalConfig

trait FcmMockAndApnsProvider extends AndroidAndIosProvider with FcmMockProvider with ApnsProvider {
  _: InternalConfig =>
}
