package app.softnetwork.notification.spi

/** Created by smanciot on 07/04/2018.
  */

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{InternalConfig, MailConfig, MailSettings}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.mail._
import app.softnetwork.notification.model.MailType._
import app.softnetwork.notification.model._

import java.util.Date
import javax.activation.FileDataSource
import scala.util.{Failure, Success, Try}

/** From https://gist.github.com/mariussoutier/3436111
  */
trait SimpleMailProvider extends MailProvider with MailSettings with StrictLogging {
  _: InternalConfig =>

  lazy val mailConfig: MailConfig = MailConfig

  def sendMail(notification: Mail)(implicit system: ActorSystem[_]): NotificationAck = {

    val format =
      if (notification.attachment.nonEmpty || notification.attachments.nonEmpty) MultiPart
      else if (notification.richMessage.isDefined) Rich
      else Plain

    val commonsMail: Email = format match {
      case Rich =>
        new HtmlEmail()
          .setHtmlMsg(notification.richMessage.getOrElse(notification.message))
          .setTextMsg(notification.message)
      case MultiPart =>
        val multipart = new HtmlEmail()
        val attachments = notification.attachments.toList ++ {
          notification.attachment match {
            case Some(attachment) => List(attachment)
            case _                => List.empty
          }
        }
        attachments.foreach(attachment => {
          multipart.attach(
            new FileDataSource(attachment.path),
            attachment.name,
            attachment.description.orNull,
            EmailAttachment.ATTACHMENT
          )
        })
        multipart
          .setHtmlMsg(notification.richMessage.getOrElse(notification.message))
          .setTextMsg(notification.message)

      case _ => new SimpleEmail().setMsg(notification.message)
    }

    // Set authentication
    commonsMail.setHostName(mailConfig.host)
    commonsMail.setSmtpPort(mailConfig.port)
    commonsMail.setSslSmtpPort(mailConfig.sslPort.toString)
    if (mailConfig.username.nonEmpty) {
      commonsMail.setAuthenticator(
        new DefaultAuthenticator(mailConfig.username, mailConfig.password)
      )
    }
    commonsMail.setSSLOnConnect(mailConfig.sslEnabled)
    commonsMail.setSSLCheckServerIdentity(mailConfig.sslCheckServerIdentity)
    commonsMail.setStartTLSEnabled(mailConfig.startTLSEnabled)
    commonsMail.setSocketConnectionTimeout(mailConfig.socketConnectionTimeout)
    commonsMail.setSocketTimeout(mailConfig.socketTimeout)

    // Can't add these via fluent API because it produces exceptions
    notification.to.foreach(commonsMail.addTo)
    notification.cc.foreach(commonsMail.addCc)
    notification.bcc.foreach(commonsMail.addBcc)

    Try(
      commonsMail
        .setFrom(notification.from.value, notification.from.alias.getOrElse(""))
        .setSubject(notification.subject) // MimeUtility.encodeText(subject, "utf-8", "B")
        .send()
    ) match {
      case Success(s) =>
        logger.info(s)
        NotificationAck(
          Some(s),
          notification.to.map(recipient =>
            NotificationStatusResult(recipient, NotificationStatus.Sent, None, Some(s))
          ),
          new Date()
        )
      case Failure(f) =>
        logger.error(f.getMessage, f)
        NotificationAck(
          None,
          notification.to.map(recipient =>
            NotificationStatusResult(recipient, NotificationStatus.Undelivered, Some(f.getMessage))
          ),
          new Date()
        )
    }
  }

}
