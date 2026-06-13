package com.example.p2pandroidp2pandroid

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ApiClient {
    companion object {
        private const val TAG = "ApiClient"
        private const val TIMEOUT_MS: Long = 15000

        // Toggle test mode: sends ORDS test_connect instead of real Firebase flow
        var TEST_MODE = true
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .header("User-Agent", "Android")
                .build()

            chain.proceed(request)
        }

        .connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    // --- User Management ---
    fun registerUser(userId: String, name: String, token: String, callback: (Boolean) -> Unit) {
        val url = ServerConfig.USERS + userId
        val json = JSONObject().put("user_id", userId).put("name", name).put("fcm_token", token)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).put(body).build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    fun getUser(userId: String, callback: (Map<String, String?>) -> Unit) {
        val request = Request.Builder().url(ServerConfig.USERS + userId).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val map = mutableMapOf<String, String?>()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    map["fcm_token"] = json.optString("fcm_token")
                    map["name"] = json.optString("name")
                }
                callback(map)
            }
            override fun onFailure(call: Call, e: IOException) { callback(emptyMap()) }
        })
    }

    // --- Call Management ---
    fun initiateCall(callerId: String, calleeId: String, callback: (String?) -> Unit) {
        val json = JSONObject().put("caller_id", callerId).put("callee_id", calleeId)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(ServerConfig.CALLS).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val sessionId = if (response.isSuccessful) {
                    JSONObject(response.body?.string() ?: "{}").optString("session_id")
                } else null
                callback(sessionId)
            }
            override fun onFailure(call: Call, e: IOException) { callback(null) }
        })
    }

    // --- Test Connect (ORDS test endpoint, no real FCM token needed) ---
    fun testConnect(callerId: String, calleeId: String, callback: (String?) -> Unit) {
        Log.d(TAG, "TEST_MODE: calling ${ServerConfig.TEST_CONNECT} caller=$callerId callee=$calleeId")
        val json = JSONObject().put("caller_id", callerId).put("callee_id", calleeId)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(ServerConfig.TEST_CONNECT)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val sessionId = if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "{}"
                    Log.d(TAG, "TEST_MODE response: $bodyStr")
                    JSONObject(bodyStr).optString("session_id")
                } else {
                    Log.e(TAG, "TEST_MODE failed: ${response.code}")
                    null
                }
                callback(sessionId)
            }
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "TEST_MODE network error: ${e.message}")
                callback(null)
            }
        })
    }

    fun getSession(sessionId: String, callback: (JSONObject?) -> Unit) {
        val request = Request.Builder().url(ServerConfig.SESSION + sessionId + "/").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    callback(json)
                } else {
                    callback(null)
                }
            }
            override fun onFailure(call: Call, e: IOException) { callback(null) }
        })
    }

    fun updateCallStatus(sessionId: String, status: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().put("status", status)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(ServerConfig.SESSION + sessionId + "/")
            .put(body)
            .build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    // --- SDP ---
    fun saveOffer(sessionId: String, sdp: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().put("session_id", sessionId).put("sdp", sdp)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(ServerConfig.SDP_OFFERS).post(body).build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    fun getOffer(sessionId: String, callback: (String?) -> Unit) {
        val request = Request.Builder().url(ServerConfig.SDP_OFFERS + sessionId + "/").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val sdp = if (response.isSuccessful) {
                    JSONObject(response.body?.string() ?: "{}").optString("offer_sdp")
                } else null
                callback(sdp)
            }
            override fun onFailure(call: Call, e: IOException) { callback(null) }
        })
    }

    fun saveAnswer(sessionId: String, sdp: String, callback: (Boolean) -> Unit) {
        val json = JSONObject().put("session_id", sessionId).put("sdp", sdp)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(ServerConfig.SDP_ANSWERS).post(body).build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    fun getAnswer(sessionId: String, callback: (String?) -> Unit) {
        val request = Request.Builder().url(ServerConfig.SDP_ANSWERS + sessionId + "/").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val sdp = if (response.isSuccessful) {
                    JSONObject(response.body?.string() ?: "{}").optString("answer_sdp")
                } else null
                callback(sdp)
            }
            override fun onFailure(call: Call, e: IOException) { callback(null) }
        })
    }

    // --- ICE Candidates ---
    fun addCandidate(
        sessionId: String,
        candidate: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject()
            .put("session_id", sessionId)
            .put("candidate", candidate)
            .put("sdp_mid", sdpMid)
            .put("sdp_mline_index", sdpMLineIndex)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(ServerConfig.ICE_CANDIDATES).post(body).build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    fun getCandidates(sessionId: String, callback: (List<Map<String, Any?>>) -> Unit) {
        val request = Request.Builder()
            .url(ServerConfig.ICE_CANDIDATES + sessionId + "/")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val list = mutableListOf<Map<String, Any?>>()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null) {
                        for (i in 0 until candidates.length()) {
                            val c = candidates.getJSONObject(i)
                            list.add(mapOf(
                                "candidate" to c.optString("candidate"),
                                "sdp_mid" to c.optString("sdp_mid"),
                                "sdp_mline_index" to c.optInt("sdp_mline_index")
                            ))
                        }
                    }
                }
                callback(list)
            }
            override fun onFailure(call: Call, e: IOException) { callback(emptyList()) }
        })
    }

    // --- Health Check ---
    fun healthCheck(callback: (Boolean) -> Unit) {
        val request = Request.Builder().url(ServerConfig.HEALTH).get().build()
        client.newCall(request).enqueue(createCallback(callback))
    }

    private fun createCallback(callback: (Boolean) -> Unit): Callback = object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) Log.d(TAG, "Success: ${response.body?.string()}")
            callback(response.isSuccessful)
        }
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Error: ${e.message}")
            callback(false)
        }
    }
}
