package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.persistence.jdbc.schema.{JdbcSchemaProvider, JdbcSchemaTypes}
import app.softnetwork.persistence.schema.SchemaType
import app.softnetwork.session.service.SessionEndpoints
import com.softwaremill.session.CsrfCheckHeaderAndForm
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsWithOneOffCookieSchedulerEndpointsPostgresLauncher
    extends AllNotificationsWithSchedulerEndpointsApi
    with JdbcSchemaProvider
    with CsrfCheckHeaderAndForm {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override def schemaType: SchemaType = JdbcSchemaTypes.Postgres

  override def sessionEndpoints: ActorSystem[_] => SessionEndpoints = system =>
    SessionEndpoints.oneOffCookie(system, checkHeaderAndForm)
}
