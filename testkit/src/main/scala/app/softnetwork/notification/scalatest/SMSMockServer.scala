package app.softnetwork.notification.scalatest

import akka.Done
import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.scalatest.MockServer
import app.softnetwork.notification.config.{InternalConfig, SMSSettings}
import app.softnetwork.persistence.generateUUID
import com.typesafe.scalalogging.StrictLogging
import org.rapidoid.buffer.Buf
import org.rapidoid.http.{AbstractHttpServer, HttpStatus, MediaType}
import org.rapidoid.net.Server
import org.rapidoid.net.abstracts.Channel
import org.rapidoid.net.impl.RapidoidHelper

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait SMSMockServer extends AbstractHttpServer with MockServer with SMSSettings with StrictLogging {
  _: InternalConfig =>

  val name: String = "sms"

  implicit def system: ActorSystem[_]

  def serverPort: Int

  override def handle(ctx: Channel, buf: Buf, data: RapidoidHelper): HttpStatus = {
    if (
      data.isGet.value && matches(
        buf,
        data.path,
        s"/${SMSConfig.mode.map(_.version).getOrElse("")}/sendSMS.do".getBytes
      )
    ) {
      ok(
        ctx,
        data.isKeepAlive.value,
        s"0 | TEXT | ${generateUUID()}".getBytes,
        MediaType.TEXT_PLAIN
      )
    } else if (
      data.isGet.value && matches(
        buf,
        data.path,
        s"/${SMSConfig.mode.map(_.version).getOrElse("")}/compteRendu.do".getBytes
      )
    ) {
      ok(ctx, data.isKeepAlive.value, s"0612345678 0".getBytes, MediaType.TEXT_PLAIN)
    } else
      HttpStatus.NOT_FOUND
  }

  private[this] lazy val maybeServer: Option[Server] =
    Try(listen(serverPort)) match {
      case Success(server) => Some(server)
      case Failure(f) =>
        logger.error(s"Could not start mock server $name at $serverPort -> ${f.getMessage}")
        None
    }

  protected override def start(): Boolean = {
    maybeServer match {
      case Some(_) => true
      case _       => false
    }
  }

  protected override def stop(): Future[Done] = {
    maybeServer match {
      case Some(server) => server.shutdown()
      case _            =>
    }
    Future.successful(Done)
  }
}
