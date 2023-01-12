package app.softnetwork.notification.scalatest

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream.Subscribe
import app.softnetwork.notification.api.NotificationClient
import app.softnetwork.notification.config.MailSettings
import app.softnetwork.notification.config.NotificationSettings.NotificationConfig
import app.softnetwork.notification.launch.NotificationGuardian
import app.softnetwork.notification.message.Schedule4NotificationTriggered
import app.softnetwork.notification.model.Notification
import app.softnetwork.scheduler.scalatest.SchedulerTestKit
import com.typesafe.config.Config
import org.scalatest.Suite
import app.softnetwork.notification.model.{BasicDevice, From, Mail, Platform, Push, SMS}
import app.softnetwork.persistence.launch.PersistentEntity
import app.softnetwork.persistence.query.EventProcessorStream

import java.net.ServerSocket

trait NotificationTestKit[T <: Notification]
    extends SchedulerTestKit
    with NotificationGuardian[T]
    with ApnsToken {
  _: Suite =>

  implicit lazy val asystem: ActorSystem[_] = typedSystem()

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

  val subject = "test"
  val message = "message"

  protected def generateMail(uuid: String): Mail =
    Mail.defaultInstance
      .withUuid(uuid)
      .withFrom(From(MailSettings.MailConfig.username, None))
      .withTo(Seq("nobody@gmail.com"))
      .withSubject(subject)
      .withMessage(message)

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

  val iosDevice: BasicDevice = BasicDevice(generateRandomDeviceToken, Platform.IOS)

  val androidDevice: BasicDevice = BasicDevice("test-token-android", Platform.ANDROID)

  val probe: TestProbe[Schedule4NotificationTriggered] =
    createTestProbe[Schedule4NotificationTriggered]()
  asystem.eventStream.tell(Subscribe(probe.ref))

  lazy val client: NotificationClient = NotificationClient(asystem)

  override def entities: ActorSystem[_] => Seq[PersistentEntity[_, _, _, _]] = sys =>
    schedulerEntities(sys) ++ notificationEntities(sys)

  override def eventProcessorStreams: ActorSystem[_] => Seq[EventProcessorStream[_]] = sys =>
    schedulerEventProcessorStreams(sys) ++
    notificationEventProcessorStreams(sys)
}
