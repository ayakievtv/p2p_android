package com.example.p2pandroidp2pandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.p2pandroidp2pandroid.ui.theme.P2PAndroidTheme
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

// -------------------- ACTIVITY --------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            P2PAndroidTheme {
                CallScreen()
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }
}

// -------------------- MATCH ENGINE --------------------

class MatchEngine(private val api: ApiClient) {

    private val handler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    fun start(userId: String, onMatched: (String, Role) -> Unit) {

        running.set(true)

        val task = object : Runnable {
            override fun run() {

                if (!running.get()) return

                api.match(userId) { json ->

                    val status = json?.optString("status")

                    if (status == "matched") {

                        running.set(false)

                        val sessionId = json!!.optString("session_id")
                        val roleStr = json.optString("role")

                        val role = if (roleStr == "caller") Role.CALLER else Role.CALLEE

                        onMatched(sessionId, role)

                    } else {
                        val sessionId = json!!.optString("session_id")
                        val roleStr = json.optString("role")
                        handler.postDelayed(this, 1200)
                    }
                }
            }
        }

        handler.post(task)
    }

    fun stop() {
        running.set(false)
    }
}

// -------------------- UI --------------------

@Composable
fun CallScreen() {

    val api = remember { ApiClient() }
    val engine = remember { MatchEngine(api) }

    var userId by remember { mutableStateOf("mama") }

    var status by remember { mutableStateOf(CallStatus.IDLE) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf<Role?>(null) }

    var incomingVisible by remember { mutableStateOf(false) }
    var incomingCaller by remember { mutableStateOf("") }

    fun start() {

        status = CallStatus.CONNECTING

        engine.start(userId) { sid, r ->

            sessionId = sid
            role = r

            when (r) {
                Role.CALLER -> {
                    status = CallStatus.RINGING
                    startOfferFlow(sid)
                }

                Role.CALLEE -> {
                    status = CallStatus.IN_PROGRESS
                    startAnswerFlow(sid)
                }
            }
        }
    }

    fun hangup() {
        status = CallStatus.ENDED
        engine.stop()
        sessionId = null
        role = null
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("User ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text("Status: $status")
            Text("Session: ${sessionId ?: "-"}")
            Text("Role: ${role ?: "-"}")

            Spacer(Modifier.height(24.dp))

            Row {

                Button(
                    enabled = status == CallStatus.IDLE,
                    onClick = { start() }
                ) { Text("Start matching") }

                Spacer(Modifier.width(12.dp))

                Button(
                    enabled = status != CallStatus.IDLE,
                    onClick = { hangup() }
                ) { Text("Hang up") }
            }
        }
    }

    if (incomingVisible) {
        IncomingCallDialog(
            callerId = incomingCaller,
            onAccept = {
                incomingVisible = false
                status = CallStatus.IN_PROGRESS
            },
            onReject = {
                incomingVisible = false
                hangup()
            }
        )
    }
}

// -------------------- WEBRTC STUB --------------------

fun startOfferFlow(sessionId: String) {
    // MINIMAL FLOW:
    // 1. createOffer()
    // 2. saveOffer(sessionId)
    // 3. pollAnswer(sessionId)

    println("Offer flow started: $sessionId")
}

fun startAnswerFlow(sessionId: String) {
    // 1. pollOffer(sessionId)
    // 2. createAnswer()
    // 3. saveAnswer(sessionId)

    println("Answer flow started: $sessionId")
}

// -------------------- DIALOG --------------------

@Composable
fun IncomingCallDialog(
    callerId: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card {
            Column(Modifier.padding(24.dp)) {

                Text("Incoming from $callerId")

                Spacer(Modifier.height(16.dp))

                Row {

                    OutlinedButton(onClick = onReject) {
                        Text("Reject")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(onClick = onAccept) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

// -------------------- ENUMS --------------------

enum class CallStatus {
    IDLE,
    CONNECTING,
    RINGING,
    IN_PROGRESS,
    ENDED
}

enum class Role {
    CALLER,
    CALLEE
}