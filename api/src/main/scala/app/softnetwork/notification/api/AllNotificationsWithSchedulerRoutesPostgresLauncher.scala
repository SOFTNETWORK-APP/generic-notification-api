package app.softnetwork.notification.api

import app.softnetwork.persistence.jdbc.schema.{JdbcSchemaProvider, JdbcSchemaTypes}
import app.softnetwork.persistence.schema.SchemaType
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsWithSchedulerRoutesPostgresLauncher
    extends AllNotificationsWithSchedulerRoutesApi
    with JdbcSchemaProvider {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override def schemaType: SchemaType = JdbcSchemaTypes.Postgres

}
