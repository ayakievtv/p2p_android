package com.example.p2pandroidp2pandroid

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.webrtc.IceCandidate
import java.util.*

/**
 * Background service for handling calls.
 * Manages signaling and WebRTC state independently of UI.
 */
class CallService : Service() {
    companion object {
        private const val TAG = "CallService"

        // Actions
        const val ACTION_INCOMING_CALL = "com.example.p2papp.action.INCOMING_CALL"
        const val ACTION_CALL_ACCEPTED = "com.example.p2papp.action.CALL_ACCEPTED"
        const val ACTION_CALL_ENDED = "com.example.p2papp.action.CALL_ENDED"
        const val ACTION_CALL_REJECTED = "com.example.p2papp.action.CALL_REJECTED"

        // Extras
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_CALLER_ID = "caller_id"
        const val EXTRA_CALLEE_ID = "callee_id"
        const val EXTRA_STATUS = "status"
        const val EXTRA_CANDIDATE = "candidate"
    }

    private val repository = CallRepository.getInstance()
    private val apiClient = ApiClient()
    private var currentSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START_CALL -> {
                val callerId = intent.getStringExtra(EXTRA_CALLER_ID) ?: return START_STICKY
                val calleeId = intent.getStringExtra(EXTRA_CALLEE_ID) ?: return START_STICKY
                startCall(callerId, calleeId)
            }

            Actions.ACCEPT_CALL -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_STICKY
                acceptCall(sessionId)
            }

            Actions.REJECT_CALL -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_STICKY
                rejectCall(sessionId)
            }

            Actions.END_CALL -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_STICKY
                endCall(sessionId)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startCall(callerId: String, calleeId: String) {
        Log.d(TAG, "Starting call from $callerId to $calleeId")

        val calleeToken = getFcmToken(calleeId)
        if (calleeToken.isNullOrBlank()) {
            Log.e(TAG, "No FCM token for callee: $calleeId")
            return
        }

        val sessionId = UUID.randomUUID().toString()
        sendCallInvite(token = calleeToken, sessionId = sessionId, callerId = callerId)

        repository.createSession(sessionId, callerId, calleeId)
        currentSessionId = sessionId
        repository.updateSessionStatus(sessionId, CallStatus.RINGING)
    }

    private fun acceptCall(sessionId: String) {
        Log.d(TAG, "Accepting call: $sessionId")

        val session = repository.getSession(sessionId)
        session?.let {
            repository.updateSessionStatus(sessionId, CallStatus.IN_PROGRESS)
            // TODO: Create WebRTC answer
            // TODO: Send answer to caller
            notifyStatusChanged(sessionId, CallStatus.IN_PROGRESS)
        }
    }

    private fun rejectCall(sessionId: String) {
        Log.d(TAG, "Rejecting call: $sessionId")

        repository.updateSessionStatus(sessionId, CallStatus.ENDED)
        apiClient.updateCallStatus(sessionId, "rejected") { success ->
            Log.d(TAG, "Reject status updated: $success")
        }
        sendCallRejected(sessionId)
        notifyStatusChanged(sessionId, CallStatus.ENDED)
    }

    private fun endCall(sessionId: String) {
        Log.d(TAG, "Ending call: $sessionId")

        repository.updateSessionStatus(sessionId, CallStatus.ENDED)
        apiClient.updateCallStatus(sessionId, "ended") { success ->
            Log.d(TAG, "End status updated: $success")
        }
        sendCallEnded(sessionId)
        repository.clearSession(sessionId)
        currentSessionId = null
        notifyStatusChanged(sessionId, CallStatus.ENDED)
    }

    private fun sendCallInvite(token: String, sessionId: String, callerId: String) {
        // TODO: отправить FCM уведомление через серверный API
        Log.d(TAG, "Would send FCM invite to $token, session=$sessionId, caller=$callerId")
    }

    private fun sendCallRejected(sessionId: String) {
        // TODO: отправить FCM сообщение вызывающему
        Log.d(TAG, "Sending rejection for $sessionId")
    }

    private fun sendCallEnded(sessionId: String) {
        // TODO: отправить FCM сообщение удалённой стороне
        Log.d(TAG, "Sending ended for $sessionId")
    }

    private fun getFcmToken(userId: String): String? {
        // TODO: получить из профиля пользователя через API
        return "dummy_token_${UUID.randomUUID()}"
    }

    private fun notifyStatusChanged(sessionId: String, status: CallStatus) {
        val intent = Intent("call_status_changed").apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_STATUS, status.name)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}

object Actions {
    const val START_CALL = "start_call"
    const val ACCEPT_CALL = "accept_call"
    const val REJECT_CALL = "reject_call"
    const val END_CALL = "end_call"
}