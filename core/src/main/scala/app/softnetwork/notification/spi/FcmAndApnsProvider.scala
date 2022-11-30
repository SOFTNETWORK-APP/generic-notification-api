package app.softnetwork.notification.spi

trait FcmAndApnsProvider extends AndroidAndIosProvider with FcmProvider with ApnsProvider
