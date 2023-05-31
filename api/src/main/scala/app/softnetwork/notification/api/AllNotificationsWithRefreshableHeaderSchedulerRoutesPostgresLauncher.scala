package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.persistence.jdbc.schema.{JdbcSchemaProvider, JdbcSchemaTypes}
import app.softnetwork.persistence.schema.SchemaType
import app.softnetwork.session.service.SessionService
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsWithRefreshableHeaderSchedulerRoutesPostgresLauncher
    extends AllNotificationsWithSchedulerRoutesApi
    with JdbcSchemaProvider {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override def schemaType: SchemaType = JdbcSchemaTypes.Postgres

  override def sessionService: ActorSystem[_] => SessionService = system =>
    SessionService.refreshableHeader(system)
}
