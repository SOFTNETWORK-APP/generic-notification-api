package app.softnetwork.notification.api

import app.softnetwork.persistence.jdbc.schema.JdbcSchemaTypes
import app.softnetwork.persistence.schema.SchemaType
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsWithSchedulerPostgresLauncher extends AllNotificationsWithSchedulerApi {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override val schemaType: SchemaType = JdbcSchemaTypes.Postgres
}
