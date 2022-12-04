package app.softnetwork.notification.spi

trait DefaultMailAndSMSAndFcmAndIosProvider
    extends MailAndSMSAndFcmAndIosProvider
    with SimpleMailProvider
    with SMSModeProvider
    with FcmAndApnsProvider
