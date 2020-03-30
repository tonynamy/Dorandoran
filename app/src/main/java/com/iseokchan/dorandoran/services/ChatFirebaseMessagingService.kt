package com.iseokchan.dorandoran.services

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.iseokchan.dorandoran.models.Chat

class ChatFirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.notification != null) {
            val body = remoteMessage.notification!!.body
            Log.d(TAG, "Notification Body: $body")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        val auth = FirebaseAuth.getInstance()
        val rootRef = FirebaseFirestore.getInstance()
    }


    companion object {
        private const val TAG = "FCM_MESSAGE"
    }
}
