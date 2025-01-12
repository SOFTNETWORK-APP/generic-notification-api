package app.softnetwork.notification.handlers

import app.softnetwork.kv.handlers.{KvDao, KvHandler}
import app.softnetwork.notification.persistence.typed.WsClientsBehavior
import org.slf4j.{Logger, LoggerFactory}

trait WsClientsDao extends KvDao with KvHandler with WsClientsBehavior

object WsClientsDao extends WsClientsDao {
  lazy val log: Logger = LoggerFactory getLogger getClass.getName
}
