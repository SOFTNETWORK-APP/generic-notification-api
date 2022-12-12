package com.google.firebase.messaging

import java.util

trait MockFirebaseMessagingClient extends FirebaseMessagingClient {
  def messageId: Option[String] = None

  def batchResponse: Option[BatchResponse] = None

  override def send(message: Message, dryRun: Boolean): String = messageId.orNull

  override def sendAll(messages: util.List[Message], dryRun: Boolean): BatchResponse =
    batchResponse.orNull

}

object MockFirebaseMessagingClient {
  def apply(id: String): FirebaseMessagingClient = {
    new MockFirebaseMessagingClient {
      override val messageId: Option[String] = Some(id)
    }
  }

  def apply(response: BatchResponse): FirebaseMessagingClient = {
    new MockFirebaseMessagingClient {
      override val batchResponse: Option[BatchResponse] = Some(response)
    }
  }
}
