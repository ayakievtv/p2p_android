package com.example.p2pandroidp2pandroid

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {
        private const val TAG = "ApiClient"
        var TEST_MODE = true
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Android")
                    .build()
            )
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ------------------------------------------------------------
    // MATCHMAKING (MAIN FLOW)
    // ------------------------------------------------------------

    fun match(userId: String, cb: (JSONObject?) -> Unit) {

        val body = JSONObject()
            .put("user_id", userId)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(ServerConfig.MATCH)
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {

                val raw = response.body?.string() ?: "{}"

                val json = try {
                    JSONObject(raw)
                } catch (e: Exception) {
                    JSONObject()
                }

                Log.d(TAG, "match response=$json")

                cb(json)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "match error=${e.message}")
                cb(null)
            }
        })
    }

    // ------------------------------------------------------------
    // SESSION STATE
    // ------------------------------------------------------------

    fun getSession(sessionId: String, cb: (JSONObject?) -> Unit) {

        val req = Request.Builder()
            .url(ServerConfig.SESSION + sessionId + "/")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {

                val json = JSONObject(response.body?.string() ?: "{}")
                cb(json)
            }

            override fun onFailure(call: Call, e: IOException) {
                cb(null)
            }
        })
    }

    fun updateCallStatus(sessionId: String, status: String, cb: (Boolean) -> Unit) {

        val body = JSONObject()
            .put("status", status)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(ServerConfig.SESSION + sessionId + "/")
            .put(body)
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                cb(response.isSuccessful)
            }

            override fun onFailure(call: Call, e: IOException) {
                cb(false)
            }
        })
    }

    // ------------------------------------------------------------
    // SDP (WEBRTC SIGNALING)
    // ------------------------------------------------------------

    fun saveOffer(sessionId: String, sdp: String, cb: (Boolean) -> Unit) {

        val body = JSONObject()
            .put("session_id", sessionId)
            .put("sdp", sdp)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(ServerConfig.SDP_OFFERS)
            .post(body)
            .build()

        client.newCall(req).enqueue(simpleBool(cb))
    }

    fun getOffer(sessionId: String, cb: (String?) -> Unit) {

        val req = Request.Builder()
            .url(ServerConfig.SDP_OFFERS + sessionId + "/")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                cb(json.optString("offer_sdp", null))
            }

            override fun onFailure(call: Call, e: IOException) {
                cb(null)
            }
        })
    }

    fun saveAnswer(sessionId: String, sdp: String, cb: (Boolean) -> Unit) {

        val body = JSONObject()
            .put("session_id", sessionId)
            .put("sdp", sdp)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(ServerConfig.SDP_ANSWERS)
            .post(body)
            .build()

        client.newCall(req).enqueue(simpleBool(cb))
    }

    fun getAnswer(sessionId: String, cb: (String?) -> Unit) {

        val req = Request.Builder()
            .url(ServerConfig.SDP_ANSWERS + sessionId + "/")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                cb(json.optString("answer_sdp", null))
            }

            override fun onFailure(call: Call, e: IOException) {
                cb(null)
            }
        })
    }

    // ------------------------------------------------------------
    // ICE
    // ------------------------------------------------------------

    fun addCandidate(
        sessionId: String,
        candidate: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        cb: (Boolean) -> Unit
    ) {

        val body = JSONObject()
            .put("session_id", sessionId)
            .put("candidate", candidate)
            .put("sdp_mid", sdpMid)
            .put("sdp_mline_index", sdpMLineIndex)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url(ServerConfig.ICE_CANDIDATES)
            .post(body)
            .build()

        client.newCall(req).enqueue(simpleBool(cb))
    }

    fun getCandidates(sessionId: String, cb: (List<Map<String, Any?>>) -> Unit) {

        val req = Request.Builder()
            .url(ServerConfig.ICE_CANDIDATES + sessionId + "/")
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {

                val result = mutableListOf<Map<String, Any?>>()

                val json = JSONObject(response.body?.string() ?: "{}")
                val arr = json.optJSONArray("candidates")

                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        result.add(
                            mapOf(
                                "candidate" to c.optString("candidate"),
                                "sdp_mid" to c.optString("sdp_mid"),
                                "sdp_mline_index" to c.optInt("sdp_mline_index")
                            )
                        )
                    }
                }

                cb(result)
            }

            override fun onFailure(call: Call, e: IOException) {
                cb(emptyList())
            }
        })
    }

    // ------------------------------------------------------------
    // HEALTH
    // ------------------------------------------------------------

    fun health(cb: (Boolean) -> Unit) {

        val req = Request.Builder()
            .url(ServerConfig.HEALTH)
            .get()
            .build()

        client.newCall(req).enqueue(simpleBool(cb))
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private fun simpleBool(cb: (Boolean) -> Unit) = object : Callback {

        override fun onResponse(call: Call, response: Response) {
            cb(response.isSuccessful)
        }

        override fun onFailure(call: Call, e: IOException) {
            cb(false)
        }
    }
}