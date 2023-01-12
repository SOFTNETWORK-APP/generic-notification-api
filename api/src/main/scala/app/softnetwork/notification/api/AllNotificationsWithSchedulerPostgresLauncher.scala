package app.softnetwork.notification.api

import app.softnetwork.persistence.jdbc.query.PostgresSchemaProvider

object AllNotificationsWithSchedulerPostgresLauncher
    extends AllNotificationsWithSchedulerApi
    with PostgresSchemaProvider
