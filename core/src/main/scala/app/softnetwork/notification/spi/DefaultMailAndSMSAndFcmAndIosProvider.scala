package app.softnetwork.notification.spi

import app.softnetwork.notification.config.InternalConfig

trait DefaultMailAndSMSAndFcmAndIosProvider
    extends MailAndSMSAndFcmAndIosProvider
    with SimpleMailProvider
    with SMSModeProvider
    with FcmAndApnsProvider { _: InternalConfig => }
