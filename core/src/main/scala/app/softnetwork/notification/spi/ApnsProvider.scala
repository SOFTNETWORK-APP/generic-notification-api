package app.softnetwork.notification.spi

import java.io.{File => JFile}
import akka.actor.typed.ActorSystem
import app.softnetwork.concurrent.Completion
import app.softnetwork.config.{Settings => CommonSettings}
import app.softnetwork.notification.config.{ApnsConfig, PushSettings}
import com.eatthepath.pushy.apns.{ApnsClient, ApnsClientBuilder, PushNotificationResponse}
import com.eatthepath.pushy.apns.util.{
  ApnsPayloadBuilder,
  SimpleApnsPayloadBuilder,
  SimpleApnsPushNotification
}
import com.typesafe.scalalogging.StrictLogging
import org.softnetwork.notification.model.{
  NotificationStatus,
  NotificationStatusResult,
  PushPayload
}

import java.time.Duration
import scala.annotation.tailrec
import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

trait ApnsProvider extends IosProvider with Completion with StrictLogging {
  override def pushToIos(payload: PushPayload, devices: Seq[String])(implicit
    system: ActorSystem[_]
  ): Seq[NotificationStatusResult] = {
    apns(payload, devices, Seq.empty)
  }

  @tailrec
  private[this] def apns(
    payload: PushPayload,
    devices: Seq[String],
    status: Seq[NotificationStatusResult]
  )(implicit system: ActorSystem[_]): Seq[NotificationStatusResult] = {
    import ApnsProvider._

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val nbDevices: Int = devices.length
    if (nbDevices > 0) {
      val tos =
        if (nbDevices > bulkSize)
          devices.take(bulkSize)
        else
          devices

      val _config = config(payload.app)
      val _client = client(payload.app, _config)

      logger.info(
        s"""APNS -> about to send notification ${payload.title}
           |\tfor ${payload.app}
           |\tvia topic ${_config.topic}
           |\tto token(s) [${tos.mkString(",")}]
           |\tusing keystore ${_config.keystore.path}""".stripMargin
      )

      val results =
        Future.sequence(for (to <- tos) yield {
          toScala(
            _client.sendNotification(
              new SimpleApnsPushNotification(to, _config.topic, payload)
            )
          )
        }) complete () match {
          case Success(responses) =>
            for (response <- responses) yield {
              val result: NotificationStatusResult = response
              logger.info(s"send push to APNS -> $result")
              result
            }
          case Failure(f) =>
            logger.error(s"send push to APNS -> ${f.getMessage}", f)
            tos.map(to =>
              NotificationStatusResult(to, NotificationStatus.Undelivered, Some(f.getMessage))
            )
        }
      if (nbDevices > bulkSize) {
        apns(payload, devices.drop(bulkSize), status ++ results)
      } else {
        status ++ results
      }
    } else {
      logger.warn("send push to APNS -> no IOS device(s)")
      status
    }
  }
}

object ApnsProvider {
  private[this] var clients: Map[String, ApnsClient] = Map.empty

  private[this] var configs: Map[String, ApnsConfig] = Map.empty

  private[notification] def client(key: String, apnsConfig: ApnsConfig): ApnsClient = {
    clients.get(key) match {
      case Some(client) => client
      case _ =>
        val client: ApnsClient =
          clientCredentials(apnsConfig)(
            new ApnsClientBuilder()
              .setApnsServer(
                if (apnsConfig.dryRun) {
                  ApnsClientBuilder.DEVELOPMENT_APNS_HOST
                } else {
                  ApnsClientBuilder.PRODUCTION_APNS_HOST
                }
              )
          ).setConnectionTimeout(Duration.ofSeconds(CommonSettings.DefaultTimeout.toSeconds))
            .build()
        clients = clients + (key -> client)
        client
    }
  }

  private[notification] def config(key: String): ApnsConfig = {
    configs.get(key) match {
      case Some(config) => config
      case _ =>
        val config: ApnsConfig = PushSettings.AppConfigs.get(key).map(_.apns) match {
          case Some(apnsConfig) => apnsConfig
          case _                => PushSettings.DefaultConfig.apns
        }
        configs = configs + (key -> config)
        config
    }
  }

  implicit def toApnsPayload(notification: PushPayload): String = {
    val apnsPayload = new SimpleApnsPayloadBuilder()
      .setAlertTitle(notification.title)
      .setAlertBody(notification.body)
      .setSound(notification.sound.getOrElse(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME))
    if (notification.badge > 0) {
      apnsPayload.setBadgeNumber(notification.badge)
    }
    apnsPayload.build()
  }

  implicit def toNotificationStatusResult(
    result: PushNotificationResponse[SimpleApnsPushNotification]
  ): NotificationStatusResult = {
    val error = Option(result.getRejectionReason) match {
      case Some(e) => Some(s"${result.getPushNotification.getToken} -> $e")
      case _       => None
    }
    NotificationStatusResult(
      result.getPushNotification.getToken,
      if (result.isAccepted)
        NotificationStatus.Sent
      else
        NotificationStatus.Rejected,
      error
    )
  }

  private[notification] def clientCredentials(
    apnsConfig: ApnsConfig
  ): ApnsClientBuilder => ApnsClientBuilder = builder => {
    val file = new JFile(apnsConfig.keystore.path)
    if (file.exists) {
      builder.setClientCredentials(file, apnsConfig.keystore.password)
    } else {
      builder.setClientCredentials(
        getClass.getClassLoader.getResourceAsStream(apnsConfig.keystore.path),
        apnsConfig.keystore.password
      )
    }
  }

}
