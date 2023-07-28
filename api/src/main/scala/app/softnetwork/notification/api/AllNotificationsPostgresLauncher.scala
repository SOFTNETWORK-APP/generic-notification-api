package app.softnetwork.notification.api

import akka.actor.typed.ActorSystem
import app.softnetwork.api.server.ApiRoute
import app.softnetwork.persistence.jdbc.schema.{JdbcSchemaProvider, JdbcSchemaTypes}
import app.softnetwork.persistence.schema.SchemaType
import org.slf4j.{Logger, LoggerFactory}

object AllNotificationsPostgresLauncher extends AllNotificationsApi with JdbcSchemaProvider {

  lazy val log: Logger = LoggerFactory getLogger getClass.getName

  override def schemaType: SchemaType = JdbcSchemaTypes.Postgres

  override def apiRoutes: ActorSystem[_] => List[ApiRoute] = _ => List.empty

}
