package app.softnetwork.notification.api

import app.softnetwork.persistence.jdbc.schema.{JdbcSchemaProvider, JdbcSchemaTypes}
import app.softnetwork.persistence.schema.SchemaType
import app.softnetwork.session.CsrfCheckHeader
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsWithSchedulerEndpointsPostgresLauncher
    extends AllNotificationsWithSchedulerEndpointsApi
    with JdbcSchemaProvider
    with CsrfCheckHeader {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override def schemaType: SchemaType = JdbcSchemaTypes.Postgres

}
