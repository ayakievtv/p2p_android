package com.example.p2pandroidp2pandroid

import android.util.Log
import org.webrtc.IceCandidate
import java.util.concurrent.ConcurrentHashMap

class CallRepository private constructor() {
    companion object {
        private const val TAG = "CallRepository"
        @Volatile
        private var INSTANCE: CallRepository? = null

        fun getInstance(): CallRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallRepository().also { INSTANCE = it }
            }
        }
    }

    private val sessions = ConcurrentHashMap<String, CallSession>()
    private val pendingCandidates = ConcurrentHashMap<String, MutableList<IceCandidate>>()
    private val apiClient = ApiClient()

    fun createSession(
        sessionId: String,
        callerId: String,
        calleeId: String,
        fcmToken: String? = null
    ): CallSession {
        val session = CallSession(
            sessionId = sessionId,
            callerId = callerId,
            calleeId = calleeId,
            status = CallStatus.CONNECTING,
            fcmToken = fcmToken
        )
        sessions[sessionId] = session
        pendingCandidates[sessionId] = mutableListOf()
        Log.d(TAG, "Created session: $sessionId")
        return session
    }

    fun updateSessionStatus(sessionId: String, status: CallStatus) {
        sessions[sessionId]?.status = status
        apiClient.updateCallStatus(sessionId, status.name.lowercase()) { success ->
            Log.d(TAG, "Status update $sessionId: $success")
        }
    }

    fun addIceCandidate(sessionId: String, candidate: IceCandidate) {
        val list = pendingCandidates.getOrPut(sessionId) { mutableListOf() }
        list.add(candidate)
        Log.d(TAG, "Added candidate for $sessionId")
        apiClient.addCandidate(
            sessionId = sessionId,
            candidate = candidate.sdp,
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex
        ) { success ->
            Log.d(TAG, "Candidate sent for $sessionId: $success")
        }
    }

    fun getPendingCandidates(sessionId: String): List<IceCandidate> {
        return pendingCandidates[sessionId]?.toList() ?: emptyList()
    }

    fun clearSession(sessionId: String) {
        sessions.remove(sessionId)
        pendingCandidates.remove(sessionId)
        Log.d(TAG, "Cleared session: $sessionId")
    }

    fun getSession(sessionId: String): CallSession? {
        return sessions[sessionId]
    }

    fun getSessionIdForUser(userId: String): String? {
        return sessions.values.find {
            it.calleeId == userId || it.callerId == userId
        }?.sessionId
    }

    fun getCalleeToken(calleeId: String, callback: (String?) -> Unit) {
        val sessionId = getSessionIdForUser(calleeId)
        if (sessionId == null) {
            callback(null)
            return
        }
        apiClient.getSession(sessionId) { json ->
            callback(json?.optString("fcm_token"))
        }
    }
}

data class CallSession(
    val sessionId: String,
    val callerId: String,
    val calleeId: String,
    var status: CallStatus,
    val fcmToken: String? = null,
    var offerSdp: String? = null,
    var answerSdp: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isActive() = status == CallStatus.IN_PROGRESS
    fun isCaller(userId: String) = callerId == userId
    fun isCallee(userId: String) = calleeId == userId
}