package app.softnetwork.notification.spi

import app.softnetwork.notification.config.InternalConfig

trait DefaultMailAndSMSAndFcmAndIosAndWsProvider
    extends MailAndSMSAndFcmAndIosAndWsProvider
    with SimpleMailProvider
    with SMSModeProvider
    with FcmAndApnsProvider { _: InternalConfig => }
