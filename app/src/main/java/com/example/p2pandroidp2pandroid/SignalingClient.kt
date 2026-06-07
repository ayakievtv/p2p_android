package com.example.p2pandroidp2pandroid

import android.util.Log
import okhttp3.*
import okhttp3.MediaType. Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

/**
 * WebSocket-based signaling client for real-time message exchange.
 * Falls back to HTTP polling if WebSocket unavailable.
 */
class SignalingClient(
    private val serverUrl: String = "https://your-server.com/ords/yakiev/iptel"
) {
    companion object {
        private const val TAG = "SignalingClient"
        private const val WS_ENDPOINT = "wss://your-server.com/ws"
    }
    
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val messageQueue = ConcurrentLinkedQueue<PendingMessage>()
    private var isConnected = false
    private var pollingJob: Thread? = null
    
    // Listeners
    private val listeners = mutableListOf<SignalListener>()
  

    interface SignalListener {
        fun onMessage(message: SignalMessage)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
  
    data class SignalMessage(
        val type: String,
        val sessionId: String,
        val payload: JSONObject,
        val timestamp: Long = System.currentTimeMillis()
    )
  
    data class PendingMessage(
        val type: String,
        val sessionId: String,
        val payload: JSONObject,
        val attempts: Int = 0
    )
  
    fun connect() {
        Log.d(TAG, "Connecting to signaling server")
      
        // Try WebSocket first
        try {
            val request = Request.Builder()
                .url(WS_ENDPOINT)
                .build()
        
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    Log.d(TAG, "WebSocket connected")
                    isConnected = true
                    notifyConnected()
                }
      
                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    Log.d(TAG, "Received: $text")
                    handleMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    Log.e(TAG, "WebSocket failure: ${t.message}, response: ${response?.code}")
                    isConnected = false
                    startPolling()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket init failed, using polling", e)
            startPolling()
        }
    }
  
    fun disconnect() {
        webSocket?.close(1000, "Goodbye!")
        webSocket = null
        isConnected = false
        pollingJob?.interrupt()
    }
  
    fun sendMessage(type: String, sessionId: String, payload: JSONObject) {
        val message = PendingMessage(type, sessionId, payload)
      
        if (isConnected) {
            sendViaWebSocket(message)
        } else {
            messageQueue.offer(message)
            sendPendingMessages()
        }
    }
  
    private fun sendViaWebSocket(message: PendingMessage) {
        val json = JSONObject().apply {
            put("type", message.type)
            put("session_id", message.sessionId)
            put("payload", message.payload)
            put("timestamp", System.currentTimeMillis())
        }
      
        val body = json.toString()
        val requestBody = body.toRequestBody("application/json".toMediaType())

        webSocket?.send(requestBody.toString())
    }
  
    private fun startPolling() {
        pollingJob = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    sendPendingMessages()
                    Thread.sleep(2000 + Random.nextLong(3000)) // 2-5s interval
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }
  
    private fun sendPendingMessages() {
        val iterator = messageQueue.iterator()
        while (iterator.hasNext()) {
            val message = iterator.next()
            if (message.attempts < 3) {
                sendHttpRequest(message)
                messageQueue.remove(message)
            }
        }
    }
  
    private fun sendHttpRequest(message: PendingMessage) {
        val url = "$serverUrl/api/signal"
        val json = JSONObject().apply {
            put("type", message.type)
            put("session_id", message.sessionId)
            put("payload", message.payload)
        }
      
        val body = json.toString().toRequestBody("application/json".toMediaType())
      
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
      
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Message sent: ${message.type}")
                } else {
                    Log.e(TAG, "Failed to send: ${response.code}")
                }
            }
      
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error: ${e.message}")
            }
        })
    }
  
    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val message = SignalMessage(
                type = json.optString("type"),
                sessionId = json.optString("session_id"),
                payload = json.optJSONObject("payload") ?: JSONObject()
            )
            Log.d(TAG, "Handled message: ${message.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Invalid message format", e)
        }
    }
  
    fun addListener(listener: SignalListener) {
        listeners.add(listener)
    }
  
    fun removeListener(listener: SignalListener) {
        listeners.remove(listener)
    }
  
    private fun notifyConnected() {
        listeners.forEach { it.onConnected() }
    }
}
