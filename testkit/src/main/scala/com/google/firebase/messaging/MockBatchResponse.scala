package com.google.firebase.messaging

import scala.jdk.CollectionConverters._

import java.util

object MockBatchResponse {
  def apply(messageId: String*): BatchResponse = {
    new BatchResponse {
      override def getResponses: util.List[SendResponse] =
        messageId.map(SendResponse.fromMessageId).toList.asJava

      override def getSuccessCount: Int = getResponses.size()

      override def getFailureCount: Int = 0
    }
  }
}
