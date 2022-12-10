package app.softnetwork.notification.spi

import app.softnetwork.notification.config.{FcmConfig, InternalConfig}
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.{
  FirebaseMessaging,
  MockBatchResponse,
  MockFirebaseMessaging,
  MockFirebaseMessagingClient,
  MockGoogleCredentials
}

trait FcmMockProvider extends FcmProvider { _: InternalConfig =>

  override protected def credentials(config: FcmConfig): GoogleCredentials = {
    MockGoogleCredentials("test-token")
  }

  override protected def additionalOptions(
    config: FcmConfig
  ): FirebaseOptions.Builder => FirebaseOptions.Builder = builder => {
    builder.setProjectId("test-project")
  }

  override protected def messaging(key: String, config: FcmConfig): FirebaseMessaging = {
    MockFirebaseMessaging(
      app(key, config),
      MockFirebaseMessagingClient(MockBatchResponse("test-response"))
    )
  }
}
