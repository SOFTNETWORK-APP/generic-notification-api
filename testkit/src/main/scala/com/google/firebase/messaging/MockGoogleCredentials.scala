package com.google.firebase.messaging

import com.google.auth.oauth2.{AccessToken, GoogleCredentials}

import java.util.Date
import java.util.concurrent.TimeUnit

trait MockGoogleCredentials extends GoogleCredentials {

  def tokenValue: String

  lazy val expiryTime: Long = System.currentTimeMillis + TimeUnit.HOURS.toMillis(1)

  override def refreshAccessToken(): AccessToken = {
    new AccessToken(tokenValue, new Date(expiryTime))
  }

}

object MockGoogleCredentials {
  def apply(token: String): GoogleCredentials = {
    new MockGoogleCredentials {
      override def tokenValue: String = token
    }
  }
}
