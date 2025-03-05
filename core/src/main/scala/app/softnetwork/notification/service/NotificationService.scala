package app.softnetwork.notification.service

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.CompletionStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import app.softnetwork.api.server.{ApiRoute, DefaultComplete}
import app.softnetwork.notification.config.NotificationSettings
import app.softnetwork.notification.spi.{WsChannels, WsClients, WsSessions}
import app.softnetwork.serialization.commonFormats
import app.softnetwork.session.config.Settings
import app.softnetwork.session.model.{SessionData, SessionDataCompanion, SessionDataDecorator}
import app.softnetwork.session.service.{SessionMaterials, SessionService}
import com.softwaremill.session.CsrfDirectives.{hmacTokenCsrfProtection, setNewCsrfToken}
import com.softwaremill.session.CsrfOptions.checkHeader
import com.softwaremill.session.SessionConfig
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson.Serialization
import org.json4s.{jackson, Formats}

trait NotificationService[SD <: SessionData with SessionDataDecorator[SD]]
    extends Directives
    with DefaultComplete
    with Json4sSupport
    with SessionService[SD]
    with ApiRoute { self: SessionMaterials[SD] =>

  implicit def serialization: Serialization.type = jackson.Serialization

  implicit def formats: Formats = commonFormats

  implicit def companion: SessionDataCompanion[SD]

  implicit def sessionConfig: SessionConfig = Settings.Session.DefaultSessionConfig

  implicit def system: ActorSystem[_]

  override implicit def ts: ActorSystem[_] = system

  val route: Route = pathPrefix(NotificationSettings.NotificationConfig.path) {
    path("connect" / Segment) { clientId =>
      requiredSession(sc, gt) { session =>
        parameters("channel".optional) { channel =>
          session.get("channels").getOrElse("").split(",").filter(_.nonEmpty).toSet.foreach { ch =>
            WsChannels.addSession(ch, session.id)
          }
          handleWebSocketMessages(websocketFlow(session.id, clientId, channel))
        }
      }
    } ~
    path("channels" / Segment) { channel =>
      // check anti CSRF token
      hmacTokenCsrfProtection(checkHeader) {
        // check if a session exists
        requiredSession(sc, gt) { session =>
          post {
            WsChannels.addSession(channel, session.id)
            complete(HttpResponse(StatusCodes.OK))
            /*val channels =
              session.get("channels").getOrElse("").split(",").filter(_.nonEmpty).toSet + channel
            var updatedSession = session
            updatedSession += ("channels" -> channels.mkString(","))
            setSession(sc, st, updatedSession) {
              // create a new anti csrf token
              setNewCsrfToken(checkHeader) {
                complete(HttpResponse(StatusCodes.OK))
              }
            }*/
          } ~ delete {
            WsChannels.removeSession(channel, session.id)
            complete(HttpResponse(StatusCodes.OK))
            /*val channels =
              session.get("channels").getOrElse("").split(",").filter(_.nonEmpty).toSet - channel
            var updatedSession = session
            updatedSession += ("channels" -> channels.mkString(","))
            setSession(sc, st, updatedSession) {
              // create a new anti csrf token
              setNewCsrfToken(checkHeader) {
                complete(HttpResponse(StatusCodes.OK))
              }
            }*/
          }
        }
      }
    }
  }

  private def websocketFlow(
    sessionId: String,
    clientId: String,
    channel: Option[String] = None
  ): Flow[Message, Message, Any] = {
    val completionMatcher: PartialFunction[Any, CompletionStrategy] = {
      case akka.actor.Status.Success(s: CompletionStrategy) => s
      case akka.actor.Status.Success(_)                     => CompletionStrategy.draining
      case akka.actor.Status.Success                        => CompletionStrategy.draining
    }

    val failureMatcher: PartialFunction[Any, Throwable] = { case akka.actor.Status.Failure(cause) =>
      cause
    }

    val outgoingMessages: Source[Message, Any] = Source
      .actorRef[TextMessage.Strict](
        completionMatcher,
        failureMatcher,
        bufferSize = 100,
        overflowStrategy = akka.stream.OverflowStrategy.dropHead
      )
      .mapMaterializedValue { actorRef =>
        WsSessions.addClient(sessionId, clientId) // add client to session
        WsClients.add(clientId, actorRef) // add actor to client
        channel match {
          case Some(ch) =>
            WsChannels.addSession(ch, sessionId) // add session to channel
            Console.out.println(
              s"Client $clientId connected to channel $channel with session $sessionId"
            )
          case _ =>
            Console.out.println(s"Client $clientId connected with session $sessionId")
        }
        actorRef
      }

    Flow
      .fromSinkAndSourceCoupled(Sink.ignore, outgoingMessages)
      .watchTermination() { (_, termination) =>
        termination.onComplete { _ =>
          Console.out.println(s"Client $clientId disconnected due to stream termination")
          WsSessions.removeClient(sessionId, clientId) // remove client from session
          WsClients.remove(clientId) // remove actor from client
          channel match {
            case Some(ch) => WsChannels.removeSession(ch, sessionId) // remove session from channel
            case _        =>
          }
        }
        NotUsed
      }

  }
}
