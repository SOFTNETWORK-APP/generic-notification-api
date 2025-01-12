package app.softnetwork.notification.persistence.typed

import app.softnetwork.kv.persistence.typed.KeyValueBehavior
import app.softnetwork.notification.config.NotificationSettings

trait WsClientsBehavior extends KeyValueBehavior {
  override def persistenceId: String = "WsClients"

  /** @return
    *   node role required to start this actor
    */
  override lazy val role: String = NotificationSettings.NotificationConfig.akkaNodeRole

}

object WsClientsBehavior extends WsClientsBehavior
