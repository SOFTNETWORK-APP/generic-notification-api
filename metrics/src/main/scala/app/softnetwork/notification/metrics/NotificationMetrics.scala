package app.softnetwork.notification.metrics

import io.prometheus.metrics.core.metrics.Counter

/** Notification delivery metrics.
  *
  * Lives in its own dependency-light `notification-metrics` module (Story 13.9) so consumers can
  * depend on it without pulling the full `notification-common` stack.
  *
  * Registers into the global
  * [[io.prometheus.metrics.model.registry.PrometheusRegistry#defaultRegistry]], so any process that
  * exposes that registry on a `/metrics` route (e.g. the softclient4es license-server, Story 13.6)
  * scrapes these series automatically — no wiring is required here.
  *
  * The counter name is declared WITHOUT the `_total` suffix — the Prometheus client appends it at
  * exposition, so the exposed series is `notification_total`.
  *
  * Labels:
  *   - `service` — always `"notification"`.
  *   - `channel` — the `NotificationType` name (e.g. `MAIL_TYPE`, `SMS_TYPE`), matching the Story
  *     13.7 audit `channel` field.
  *   - `template` — a BOUNDED template identifier, or empty when none is available at the call
  *     site. Never pass an unbounded free-text value (e.g. a raw subject): it would explode the
  *     series cardinality. The terminal send path (NotificationBehavior) has no template identifier
  *     and passes an empty value; the producer that enqueues the notification owns template
  *     attribution.
  *   - `outcome` — one of `enqueued` | `sent` | `failed` | `retried`.
  */
object NotificationMetrics {

  private val Service = "notification"

  private val notifications: Counter = Counter
    .builder()
    .name("notification")
    .help("Notifications, by channel / template / outcome")
    .labelNames("service", "channel", "template", "outcome")
    .register()

  def notification(channel: String, template: String, outcome: String): Unit =
    notifications.labelValues(Service, channel, template, outcome).inc()
}
