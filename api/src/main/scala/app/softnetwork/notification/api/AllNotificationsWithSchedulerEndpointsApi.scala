package app.softnetwork.notification.api

import app.softnetwork.persistence.schema.SchemaProvider
import app.softnetwork.scheduler.api.SchedulerEndpointsApi
import app.softnetwork.session.CsrfCheck

trait AllNotificationsWithSchedulerEndpointsApi
    extends AllNotificationsWithSchedulerApi
    with SchedulerEndpointsApi { _: SchemaProvider with CsrfCheck => }
