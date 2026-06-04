package app.softnetwork.notification.scalatest

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import app.softnetwork.notification.api.NotificationClient
import app.softnetwork.notification.config.{InternalConfig, MailSettings}
import app.softnetwork.notification.config.NotificationSettings.NotificationConfig
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.message.Schedule4NotificationTriggered
import app.softnetwork.notification.model.{
  Attachment,
  BasicDevice,
  From,
  Mail,
  Notification,
  Platform,
  Push,
  SMS,
  Ws
}
import app.softnetwork.scheduler.scalatest.SchedulerTestKit
import com.typesafe.config.Config
import org.scalatest.Suite
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream
import app.softnetwork.session.config.Settings
import org.slf4j.{Logger, LoggerFactory}
import org.softnetwork.session.model.Session

import java.net.ServerSocket
import java.nio.file.{Files, Path}
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait NotificationTestKit[T <: Notification]
    extends SchedulerTestKit
    with NotificationGuardian[T]
    with ApnsToken { _: Suite =>

  implicit def asystem: ActorSystem[_] = typedSystem()

  override protected def sessionType: Session.SessionType =
    Settings.Session.SessionContinuityAndTransport

  /** @return
    *   roles associated with this node
    */
  override def roles: Seq[String] = super.roles :+ NotificationConfig.akkaNodeRole

  lazy val internalConfig: Config = config

  def availablePort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }

  lazy val apnsPort: Int = availablePort

  lazy val apnsConfig: String = s"""
                                   |notification.push.mock.apns.port = $apnsPort
                                   |""".stripMargin

  lazy val smsPort: Int = availablePort

  lazy val smsConfig: String = s"""
                                  |notification.sms.mode.base-url = "http://$hostname:$smsPort"
                                  |""".stripMargin

  lazy val smtpPort: Int = availablePort

  lazy val smtpConfig: String = s"""
                                   |notification.mail.host = $hostname
                                   |notification.mail.port = $smtpPort
                                   |""".stripMargin

  private var apnsMockServer: Option[ApnsMockServer] = None

  protected def initApnsMockServer(coordinatedShutdown: Boolean): Unit = {
    val server = new ApnsMockServer with InternalConfig {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName

      override implicit def system: ActorSystem[_] = asystem

      override def serverPort: Int = apnsPort

      override lazy val config: Config = internalConfig
    }
    assert(
      server.init(coordinatedShutdown = coordinatedShutdown)
    )
    if (!coordinatedShutdown) {
      apnsMockServer = Some(server)
    }
  }

  protected def shutdownApnsMockServer(): Unit = {
    apnsMockServer.map(_.shutdown()).getOrElse(Future.successful(Done)).complete() match {
      case Success(_) => // do nothing
      case Failure(f) => log.error("Failed to shutdown APNs mock server", f)
    }
  }

  private var smsMockServer: Option[SMSMockServer] = None

  protected def initSmsMockServer(coordinatedShutdown: Boolean): Unit = {
    val server = new SMSMockServer with InternalConfig {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName

      override implicit def system: ActorSystem[_] = asystem

      override def serverPort: Int = smsPort

      override def config: Config = internalConfig
    }
    assert(
      server.init(coordinatedShutdown = coordinatedShutdown)
    )
    if (!coordinatedShutdown) {
      smsMockServer = Some(server)
    }
  }

  protected def shutdownSmsMockServer(): Unit = {
    smsMockServer.map(_.shutdown()).getOrElse(Future.successful(Done)).complete() match {
      case Success(_) => // do nothing
      case Failure(f) => log.error("Failed to shutdown SMS mock server", f)
    }
  }

  private var smtpMockServer: Option[SmtpMockServer] = None

  protected def receivedEmails: Seq[com.dumbster.smtp.SmtpMessage] =
    smtpMockServer.map(_.received).getOrElse(Seq.empty)
  protected def initSmtpMockServer(coordinatedShutdown: Boolean): Unit = {
    val server = new SmtpMockServer with InternalConfig {
      lazy val log: Logger = LoggerFactory getLogger getClass.getName

      override implicit def system: ActorSystem[_] = asystem

      override def serverPort: Int = smtpPort

      override lazy val config: Config = internalConfig
    }
    assert(
      server.init(coordinatedShutdown = coordinatedShutdown)
    )
    if (!coordinatedShutdown) {
      smtpMockServer = Some(server)
    }
  }

  protected def shutdownSmtpMockServer(): Unit = {
    smtpMockServer.map(_.shutdown()).getOrElse(Future.successful(Done)).complete() match {
      case Success(_) => // do nothing
      case Failure(f) => log.error("Failed to shutdown SMTP mock server", f)
    }
  }

  lazy val allMockServersConfig: String = apnsConfig + smsConfig + smtpConfig

  protected def initMockServers(coordinatedShutdown: Boolean): Unit = {
    initApnsMockServer(coordinatedShutdown)
    initSmsMockServer(coordinatedShutdown)
    initSmtpMockServer(coordinatedShutdown)
  }

  protected def shutdownMockServers(): Unit = {
    shutdownApnsMockServer()
    shutdownSmsMockServer()
    shutdownSmtpMockServer()
  }

  val subject = "test"
  val message = "message"

  protected def generateAttachment(name: String, path: Path): Attachment = {
    val bytes = Files.readAllBytes(path)
    Attachment(name, path.toString, bytes)
  }

  protected def generateMail(uuid: String, attachments: Seq[Attachment] = Seq.empty): Mail =
    Mail.defaultInstance
      .withUuid(uuid)
      .withFrom(From(MailSettings.MailConfig.username, None))
      .withTo(Seq("nobody@gmail.com"))
      .withSubject(subject)
      .withMessage(message)
      .withAttachments(attachments)

  protected def generateSMS(uuid: String): SMS =
    SMS.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)
      .withTo(Seq(uuid))

  protected def generatePush(uuid: String, devices: BasicDevice*): Push =
    Push.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)
      .withDevices(devices)
      .withApplication("mock")

  protected def generateWs(uuid: String, channel: Option[String] = None): Ws =
    Ws.defaultInstance
      .withUuid(uuid)
      .withSubject(subject)
      .withMessage(message)
      .withTo(Seq(uuid))
      .copy(channel = channel)

  val iosDevice: BasicDevice = BasicDevice(generateRandomDeviceToken, Platform.IOS)

  val androidDevice: BasicDevice = BasicDevice("test-token-android", Platform.ANDROID)

  lazy val probe: TestProbe[Schedule4NotificationTriggered] =
    createTestProbe[Schedule4NotificationTriggered]()

  lazy val client: NotificationClient = NotificationClient(asystem)

  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    schedulerEntities(sys) ++ sessionEntities(sys) ++ notificationEntities(sys)

  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    schedulerEventProcessorStreams(sys) ++
    notificationEventProcessorStreams(sys)

  def subscribeNotificationProbes(): Unit = {
    subscribeProbe(probe)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    subscribeNotificationProbes()
  }

}
