package com.example.p2pandroidp2pandroid

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

/**
 * Firebase Messaging Service for handling incoming call notifications.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        const val ACTION_CALL_INVITATION = "com.example.p2papp.ACTION_CALL_INVITATION"
        const val ACTION_CALL_ACCEPTED = "com.example.p2papp.ACTION_CALL_ACCEPTED"
        const val ACTION_CALL_REJECTED = "com.example.p2papp.ACTION_CALL_REJECTED"
        const val ACTION_CALL_ENDED = "com.example.p2papp.ACTION_CALL_ENDED"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")
        
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            
            val action = remoteMessage.data["action"]
            when (action) {
                "call_invitation" -> handleCallInvitation(remoteMessage.data)
                "call_accepted" -> handleCallAccepted(remoteMessage.data)
                "call_rejected" -> handleCallRejected(remoteMessage.data)
                "call_ended" -> handleCallEnded(remoteMessage.data)
            }
        }
        
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // Send token to server
        val apiClient = ApiClient()
        apiClient.registerUser(getUserId(), getUserId(), token) { success ->
            Log.d(TAG, "Token registered: $success")
        }
    }

    private fun handleCallInvitation(data: Map<String, String>) {
        val sessionId = data["session_id"] ?: return
        val callerId = data["caller_id"] ?: return

        Log.d(TAG, "Incoming call from: $callerId, session: $sessionId")
        
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("action", "incoming_call")
            putExtra("session_id", sessionId)
            putExtra("caller_id", callerId)
        }
        
        startActivity(intent)
    }

    private fun handleCallAccepted(data: Map<String, String>) {
        val sessionId = data["session_id"] ?: return
        Log.d(TAG, "Call accepted: $sessionId")
    }

    private fun handleCallRejected(data: Map<String, String>) {
        val sessionId = data["session_id"] ?: return
        Log.d(TAG, "Call rejected: $sessionId")
    }

    private fun handleCallEnded(data: Map<String, String>) {
        val sessionId = data["session_id"] ?: return
        Log.d(TAG, "Call ended: $sessionId")
    }

    private fun getUserId(): String {
        // TODO: Get actual user ID from preferences or auth
        return "user123"
    }
}
