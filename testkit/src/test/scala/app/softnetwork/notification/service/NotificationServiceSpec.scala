package app.softnetwork.notification.service

import akka.http.scaladsl.testkit.WSProbe
import app.softnetwork.api.server.ApiRoutes
import app.softnetwork.notification.scalatest.{AllNotificationsApiRoutesTestKit, WsClientTestKit}
import app.softnetwork.session.model.{SessionData, SessionDataDecorator}
import app.softnetwork.session.service.SessionMaterials
import org.scalatest.Suite
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success}

trait NotificationServiceSpec[SD <: SessionData with SessionDataDecorator[SD]]
    extends AnyWordSpecLike
    with AllNotificationsApiRoutesTestKit[SD] {
  _: Suite with ApiRoutes with SessionMaterials[SD] with WsClientTestKit =>

  override val refreshableSession: Boolean = false

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  val clientId = "client"
  val sessionId = "session"

  var wsClient: Option[WSProbe] = None

  "Notification service" should {

    "connect to ws server without channel" in {
      createSession(sessionId)
      wsClient = ws(clientId, sessionId)
    }

    "send message to client" in {
      val ws = generateWs(clientId)
      client.sendWs(ws) complete () match {
        case Success(result) =>
          wsClient match {
            case Some(cli) =>
              assert(result.exists(r => r.recipient == clientId && r.status.isSent))
              cli.expectMessage(ws.message)
              cli.sendCompletion()
              client.sendWs(ws) complete () match {
                case Success(result) =>
                  assert(result.exists(r => r.recipient == clientId && r.status.isRejected))
                case Failure(_) => fail()
              }
            case None =>
          }
        case Failure(_) => fail()
      }
    }

    val channel = "channel"

    "connect to ws server with channel" in {
      wsClient = ws(clientId, sessionId, Some(channel))
    }

    "send message to channel" in {
      val ws = generateWs(clientId, Some(channel)).withTo(Seq.empty)
      client.sendWs(ws) complete () match {
        case Success(result) =>
          wsClient match {
            case Some(cli) =>
              assert(result.exists(r => r.recipient == clientId && r.status.isSent))
              cli.expectMessage(ws.message)
            case None =>
          }
        case Failure(_) => fail()
      }
    }

    "disconnect from channel" in {
      removeChannel(channel)
    }

    "disconnect from ws server" in {
      wsClient match {
        case Some(cli) =>
          cli.sendCompletion()
          wsClient = None
        case None =>
      }
    }
  }
}
