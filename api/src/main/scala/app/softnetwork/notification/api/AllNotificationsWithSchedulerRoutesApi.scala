package app.softnetwork.notification.api

import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerRoutesApi

trait AllNotificationsWithSchedulerRoutesApi
    extends AllNotificationsWithSchedulerApi
    with SchedulerRoutesApi { _: SchemaProvider => }
