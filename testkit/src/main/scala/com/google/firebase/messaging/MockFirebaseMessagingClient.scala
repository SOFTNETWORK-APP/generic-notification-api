package com.google.firebase.messaging

import java.util

trait MockFirebaseMessagingClient extends FirebaseMessagingClient {
  def messageId: String

  def batchResponse: BatchResponse

  override def send(message: Message, dryRun: Boolean): String = Option(messageId).orNull

  override def sendAll(messages: util.List[Message], dryRun: Boolean): BatchResponse = Option(
    batchResponse
  ).orNull

}

object MockFirebaseMessagingClient {
  def apply(id: String): FirebaseMessagingClient = {
    new MockFirebaseMessagingClient {
      override val messageId: String = id

      override val batchResponse: BatchResponse = null
    }
  }

  def apply(response: BatchResponse): FirebaseMessagingClient = {
    new MockFirebaseMessagingClient {
      override val messageId: String = null

      override val batchResponse: BatchResponse = response
    }
  }
}
