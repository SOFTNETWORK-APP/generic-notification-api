package app.softnetwork.notification.spi

import java.io.{FileInputStream, File => JFile}
import akka.actor.typed.ActorSystem
import app.softnetwork.notification.config.{FcmConfig, PushSettings}
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.{AndroidConfig, AndroidNotification, BatchResponse, FirebaseMessaging, MulticastMessage}
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.typesafe.scalalogging.StrictLogging
import org.softnetwork.notification.model.{NotificationStatus, NotificationStatusResult, PushPayload}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait FcmProvider extends AndroidProvider with StrictLogging {
  override def pushToAndroid(payload: PushPayload, devices: Seq[String])(implicit
    system: ActorSystem[_]
  ): Seq[NotificationStatusResult] = {
    fcm(payload, devices, Seq.empty)
  }

  @tailrec
  private[this] def fcm(
    notification: PushPayload,
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

      val _config = config(notification.app)
      val _app = app(notification.app, _config)

      logger.info(
        s"""FCM -> about to send notification ${notification.title}
           |\tfor ${notification.app}
           |\tvia url ${_config.databaseUrl}
           |\tto token(s) [${tokens.mkString(",")}]
           |\tusing credentials ${_config.googleCredentials.getOrElse(
          sys.env.get("GOOGLE_APPLICATION_CREDENTIALS")
        )}""".stripMargin
      )

      val results: Seq[NotificationStatusResult] =
        Try(
          FirebaseMessaging.getInstance(_app).sendMulticast(notification)
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
        fcm(notification, devices.drop(bulkSize), status ++ results)
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

  private[notification] def app(key: String, fcmConfig: FcmConfig): FirebaseApp = {
    Try(
      FirebaseApp.getInstance(key)
    ) match {
      case Success(app) => app
      case Failure(f)   =>
//        logger.info(
//          s"${f.getMessage} -> initializing Firebase Application for $key with $fcmConfig"
//        )
        FirebaseApp.initializeApp(
          clientCredentials(fcmConfig)(FirebaseOptions.builder())
            .setDatabaseUrl(fcmConfig.databaseUrl)
            .build(),
          key
        )
    }
  }

  private[notification] def config(key: String): FcmConfig = {
    PushSettings.AppConfigs.get(key).map(_.fcm).getOrElse(PushSettings.DefaultConfig.fcm)
  }

  private[notification] def clientCredentials(
    fcmConfig: FcmConfig
  ): FirebaseOptions.Builder => FirebaseOptions.Builder =
    builder => {
      fcmConfig.googleCredentials match {
        case Some(googleCredentials) if googleCredentials.trim.nonEmpty =>
          builder.setCredentials(
            GoogleCredentials.fromStream(new FileInputStream(new JFile(googleCredentials)))
          )
        case _ => builder.setCredentials(GoogleCredentials.getApplicationDefault())
      }
    }

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
        Option(r.getException).map(e => e.getMessage)
      )
  }
}
