package app.softnetwork.notification.audit

import app.softnetwork.persistence.audit.AuditLog

object NotificationAuditLog {

  /** Story 13.7 — structured audit trail. service = "notification"; correlationId is threaded as
    * data on the notification (proto field, exposed on the Notification trait), never via MDC.
    */
  private[notification] lazy val audit: AuditLog = AuditLog("notification")

}
