package app.softnetwork.notification.spi

import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{InternalConfig, SMSMode, SMSSettings}
import org.apache.commons.text.StringEscapeUtils
import app.softnetwork.notification.model.{
  NotificationAck,
  NotificationStatus,
  NotificationStatusResult,
  SMS
}
import org.slf4j.Logger

import java.time.Instant

trait SMSModeProvider extends SMSProvider with SMSSettings { _: InternalConfig =>

  import NotificationStatus._
  import app.softnetwork.notification.config.SMSMode._
  import Status._

  import java.io.{BufferedReader, InputStreamReader}
  import java.net.{HttpURLConnection, URL, URLEncoder}
  import java.util.Date
  import scala.util.{Failure, Success, Try}

  lazy val maybeConfig: Option[SMSMode.Config] = SMSConfig.mode

  def log: Logger

  override def sendSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck = {
    import notification._

    maybeConfig match {
      case Some(conf) =>
        import conf._

        if (disabled) {
          new NotificationAck(
            None,
            to.map(recipient =>
              NotificationStatusResult(
                recipient,
                Undelivered,
                None
              )
            ),
            Instant.now()
          )
        } else {
          val sendUrl =
            s"""
               |$baseUrl/$version/sendSMS.do?
               |accessToken=$accessToken
               |&message=${URLEncoder.encode(
              StringEscapeUtils.unescapeHtml4(message).replaceAll("<br/>", "\\\n"),
              "ISO-8859-15"
            )}
               |&numero=${to.mkString(",")}
               |&emetteur=${URLEncoder.encode(from.value, "ISO-8859-15")}
               |${notificationUrl match {
              case Some(value) => s"&notification_url=$value"
              case _           => ""
            }}
               |${notificationUrlResponse match {
              case Some(value) => s"&notification_url=$value"
              case _           => ""
            }}
               |${if (stop) "&stop=2" else ""}
               |""".stripMargin.replaceAll("\\s+", "")

          log.info(sendUrl)

          val url = new URL(sendUrl)

          val connection = url.openConnection().asInstanceOf[HttpURLConnection]
          connection.setRequestMethod(
            "GET"
          ) // POST if number of recipients is greater or equal to 300
          connection.setUseCaches(false)
          connection.setDoInput(true)
          //connection.setDoOutput(true)
          connection.getResponseCode match {

            case responseCode if responseCode == 200 || responseCode == 201 =>
              Try {
                val br = new BufferedReader(new InputStreamReader(connection.getInputStream))
                Stream.continually(br.readLine()).takeWhile(_ != null).mkString("")
              } match {
                case Success(responseData) =>
                  log.info(responseData)
                  // code_retour | description | smsID
                  responseData.split("\\|").toList match {
                    case l if l.size == 3 =>
                      val smsId = l.last.trim
                      ResponseType(l.head.trim.toInt) match {
                        case ResponseType.ACCEPTED =>
                          new NotificationAck(
                            Some(smsId),
                            to.map(recipient =>
                              NotificationStatusResult(
                                recipient,
                                Pending,
                                None,
                                Some(smsId)
                              )
                            ),
                            Instant.now()
                          )
                        case _ =>
                          new NotificationAck(
                            Some(smsId),
                            to.map(recipient =>
                              NotificationStatusResult(
                                recipient,
                                Undelivered,
                                Some(l(1).trim),
                                Some(smsId)
                              )
                            ),
                            Instant.now()
                          )
                      }
                    case l if l.size == 2 =>
                      new NotificationAck(
                        None,
                        to.map(recipient =>
                          NotificationStatusResult(
                            recipient,
                            Undelivered,
                            Some(l.last.trim)
                          )
                        ),
                        Instant.now()
                      )
                    case _ =>
                      new NotificationAck(
                        None,
                        to.map(recipient =>
                          NotificationStatusResult(
                            recipient,
                            Undelivered,
                            None
                          )
                        ),
                        Instant.now()
                      )
                  }

                case Failure(f) =>
                  log.error(f.getMessage, f)
                  new NotificationAck(
                    None,
                    to.map(recipient =>
                      NotificationStatusResult(
                        recipient,
                        Undelivered,
                        Some(f.getMessage)
                      )
                    ),
                    Instant.now()
                  )
              }

            case _ =>
              new NotificationAck(
                None,
                to.map(recipient =>
                  NotificationStatusResult(
                    recipient,
                    Undelivered,
                    None
                  )
                ),
                Instant.now()
              )
          }

        }

      case None =>
        log.error("notification.sms.mode configuration has not been defined")
        new NotificationAck(
          None,
          to.map(recipient =>
            NotificationStatusResult(
              recipient,
              Undelivered,
              None
            )
          ),
          Instant.now()
        )
    }
  }

  override def ackSMS(notification: SMS)(implicit system: ActorSystem[_]): NotificationAck = {
    val results = notification.results
    val uuid = notification.ackUuid.getOrElse("")
    maybeConfig match {
      case Some(conf) =>
        import conf._
        val ackUrl = s"""
                         |$baseUrl/$version/compteRendu.do?
                         |accessToken=$accessToken
                         |&smsID=$uuid
                         |""".stripMargin.replaceAll("\\s+", "")
        log.info(ackUrl)

        val url = new URL(ackUrl)

        val connection = url.openConnection().asInstanceOf[HttpURLConnection]
        connection.setRequestMethod(
          "GET"
        ) // POST if number of recipients is greater or equal to 300
        connection.setUseCaches(false)
        connection.setDoInput(true)
        //connection.setDoOutput(true)
        connection.getResponseCode match {

          case responseCode if responseCode == 200 || responseCode == 201 =>
            Try {
              val br = new BufferedReader(new InputStreamReader(connection.getInputStream))
              Stream.continually(br.readLine()).takeWhile(_ != null).mkString("")
            } match {
              case Success(responseData) =>
                log.info(responseData)
                // numéro_destinataire statut | numéro_destinataire statut | ...
                responseData.split("\\|").toList match {
                  case Nil =>
                    NotificationAck(Some(uuid), results, Instant.now())
                  case l =>
                    NotificationAck(
                      Some(uuid),
                      l.map(i => {
                        val result = i.trim.split("\\s+").toList
                        var providerStatus: Option[String] = None
                        val status =
                          Try(Status(result.last.toInt)) match {
                            case Success(s) =>
                              providerStatus = Some(s.toString)
                              s match {
                                case SENT                 => Sent
                                case DELIVERED            => Delivered
                                case READ                 => Delivered
                                case RECEIVED             => Delivered
                                case UNREAD               => Delivered
                                case REJECTED             => Rejected
                                case INSUFFICIENT_CREDITS => Undelivered
                                case INTERNAL_ERROR       => Undelivered
                                case NOT_DELIVERABLE      => Undelivered
                                case ROUTING_ERROR        => Undelivered
                                case RECEIPT_ERROR        => Undelivered
                                case MESSAGE_ERROR        => Undelivered
                                case TOO_LONG_MESSAGE     => Undelivered
                                case _                    => Pending
                              }
                            case Failure(_) => Pending
                          }
                        val error = status match {
                          case Rejected    => providerStatus
                          case Undelivered => providerStatus
                          case _           => None
                        }
                        NotificationStatusResult(
                          if (result.size == 2)
                            result.head
                          else
                            notification.to.head,
                          status,
                          error,
                          Some(uuid)
                        )
                      }),
                      Instant.now()
                    )
                }

              case Failure(f) =>
                log.error(f.getMessage, f)
                NotificationAck(Some(uuid), results, Instant.now())
            }

          case _ => NotificationAck(Some(uuid), results, Instant.now())
        }

      case None => NotificationAck(Some(uuid), results, Instant.now())
    }
  }
}
