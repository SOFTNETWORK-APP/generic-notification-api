package app.softnetwork.notification.api

import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SwaggerSchedulerApi

trait AllNotificationsWithSwaggerSchedulerApi
    extends AllNotificationsWithSchedulerApi
    with SwaggerSchedulerApi { _: SchemaProvider => }
