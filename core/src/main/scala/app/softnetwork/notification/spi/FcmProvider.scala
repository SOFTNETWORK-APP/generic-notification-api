package app.softnetwork.notification.spi

import java.io.{File => JFile, FileInputStream}
import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{FcmConfig, PushSettings}
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.{
  AndroidConfig,
  AndroidNotification,
  BatchResponse,
  FirebaseMessaging,
  MulticastMessage
}
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.typesafe.scalalogging.StrictLogging
import org.softnetwork.notification.model.{
  NotificationStatus,
  NotificationStatusResult,
  PushPayload
}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait FcmProvider extends AndroidProvider with StrictLogging {
  final override def pushToAndroid(payload: PushPayload, devices: Seq[String])(implicit
    system: ActorSystem[_]
  ): Seq[NotificationStatusResult] = {
    val config =
      PushSettings.AppConfigs.get(payload.app).map(_.fcm).getOrElse(PushSettings.DefaultConfig.fcm)
    val firebaseMessaging: FirebaseMessaging = messaging(payload.app, config)
    fcm(config, firebaseMessaging, payload, devices, Seq.empty)
  }

  protected def credentials(
    config: FcmConfig
  ): GoogleCredentials =
    config.googleCredentials match {
      case Some(googleCredentials) if googleCredentials.trim.nonEmpty =>
        GoogleCredentials.fromStream(new FileInputStream(new JFile(googleCredentials)))
      case _ => GoogleCredentials.getApplicationDefault()
    }

  protected def additionalOptions(
    config: FcmConfig
  ): FirebaseOptions.Builder => FirebaseOptions.Builder = builder => {
    builder.setDatabaseUrl(config.databaseUrl)
  }

  final protected def app(key: String, config: FcmConfig): FirebaseApp = {
    Try(
      FirebaseApp.getInstance(key)
    ) match {
      case Success(value) => value
      case Failure(f) =>
        logger.info(
          s"${f.getMessage} -> initializing Firebase Application for $key with $config"
        )
        FirebaseApp.initializeApp(
          additionalOptions(config)(
            FirebaseOptions.builder().setCredentials(credentials(config))
          ).build,
          key
        )
    }
  }

  protected def messaging(key: String, config: FcmConfig): FirebaseMessaging = {
    FirebaseMessaging.getInstance(app(key, config))
  }

  @tailrec
  private[this] def fcm(
    config: FcmConfig,
    messaging: FirebaseMessaging,
    payload: PushPayload,
    devices: Seq[String],
    status: Seq[NotificationStatusResult]
  ): Seq[NotificationStatusResult] = {
    import FcmProvider._
    val nbDevices: Int = devices.length
    if (nbDevices > 0) {
      implicit val tokens: Seq[String] =
        if (nbDevices > bulkSize)
          devices.take(bulkSize)
        else
          devices

      logger.info(
        s"""FCM -> about to send notification ${payload.title}
           |\tfor ${payload.app}
           |\tvia url ${config.databaseUrl}
           |\tto token(s) [${tokens.mkString(",")}]
           |\tusing credentials ${config.googleCredentials.getOrElse(
          sys.env.get("GOOGLE_APPLICATION_CREDENTIALS")
        )}""".stripMargin
      )

      val results: Seq[NotificationStatusResult] =
        Try(
          messaging.sendMulticast(payload)
        ) match {
          case Success(s) =>
            val results: Seq[NotificationStatusResult] = s
            logger.info(s"send push to FCM -> ${results.mkString("|")}")
            results
          case Failure(f) =>
            logger.error(s"send push to FCM -> ${f.getMessage}", f)
            tokens.map(token =>
              NotificationStatusResult(token, NotificationStatus.Undelivered, Some(f.getMessage))
            )
        }
      if (nbDevices > bulkSize) {
        fcm(config, messaging, payload, devices.drop(bulkSize), status ++ results)
      } else {
        status ++ results
      }
    } else {
      logger.warn("send push to FCM -> no ANDROID device(s)")
      status
    }
  }

}

object FcmProvider {

  implicit def toFcmPayload(
    notification: PushPayload
  )(implicit tokens: Seq[String]): MulticastMessage = {
    val androidNotification = AndroidNotification
      .builder()
      .setTitle(notification.title)
      .setBody(notification.body)
      .setSound(notification.sound.getOrElse("default"))
    if (notification.badge > 0) {
      androidNotification.setNotificationCount(notification.badge)
    }
    val payload = MulticastMessage
      .builder()
      .setAndroidConfig(
        AndroidConfig.builder().setNotification(androidNotification.build()).build()
      )
      .addAllTokens(tokens.asJava)
      .build()
    payload
  }

  implicit def toNotificationResults(
    response: BatchResponse
  )(implicit tokens: Seq[String]): Seq[NotificationStatusResult] = {
    for ((r, i) <- response.getResponses.asScala.zipWithIndex)
      yield NotificationStatusResult(
        tokens(i),
        if (r.isSuccessful)
          NotificationStatus.Sent
        else
          NotificationStatus.Rejected,
        Option(r.getException).map(e => e.getMessage),
        Option(r.getMessageId)
      )
  }
}
