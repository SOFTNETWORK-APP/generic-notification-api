package app.softnetwork.notification.service

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.CompletionStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import app.softnetwork.api.server.ApiRoute
import app.softnetwork.notification.config.NotificationSettings
import app.softnetwork.notification.spi.WsClients

import scala.concurrent.ExecutionContext

trait NotificationService extends Directives with ApiRoute {

  implicit def system: ActorSystem[_]

  implicit lazy val ec: ExecutionContext = system.executionContext

  val route: Route = pathPrefix(NotificationSettings.NotificationConfig.path) {
    path("connect" / Segment) { clientId =>
      handleWebSocketMessages(websocketFlow(clientId))
    }
  }

  private def websocketFlow(clientId: String): Flow[Message, Message, Any] = {
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
        WsClients.addKeyValue(clientId, actorRef)
        Console.out.println(s"Client $clientId connected")
        actorRef
      }

    Flow
      .fromSinkAndSourceCoupled(Sink.ignore, outgoingMessages)
      .watchTermination() { (_, termination) =>
        termination.onComplete { _ =>
          Console.out.println(s"Client $clientId disconnected due to stream termination")
          WsClients.removeKeyValue(clientId)
        }
        NotUsed
      }

  }
}
