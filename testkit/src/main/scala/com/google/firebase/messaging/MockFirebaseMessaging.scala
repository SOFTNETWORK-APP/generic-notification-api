package com.google.firebase.messaging

import com.google.common.base.Suppliers
import com.google.firebase.FirebaseApp

object MockFirebaseMessaging {
  def apply(app: FirebaseApp, client: FirebaseMessagingClient): FirebaseMessaging = {
    FirebaseMessaging
      .builder()
      .setFirebaseApp(app)
      .setMessagingClient(Suppliers.ofInstance(client))
      .setInstanceIdClient(Suppliers.ofInstance[InstanceIdClient](null))
      .build()
  }
}
