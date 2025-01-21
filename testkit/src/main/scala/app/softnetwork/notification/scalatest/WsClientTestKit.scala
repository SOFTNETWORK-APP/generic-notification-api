package app.softnetwork.notification.scalatest

import akka.http.scaladsl.testkit.WSProbe

trait WsClientTestKit {

  def ws(clientId: String, sessionId: String, channel: Option[String] = None): Option[WSProbe] =
    None

  def addChannel(channel: String): Unit = {}

  def removeChannel(channel: String): Unit = {}
}
