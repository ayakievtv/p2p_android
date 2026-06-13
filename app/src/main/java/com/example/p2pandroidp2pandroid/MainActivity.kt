package com.example.p2pandroidp2pandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.p2pandroidp2pandroid.ui.theme.P2PAndroidTheme
import com.example.p2pandroidp2pandroid.webrtc.WebRTCManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var apiClient: ApiClient
    private var webRTCManager: WebRTCManager? = null
    private var currentSessionId: String? = null
    private var currentCallerId: String? = null
    private var currentCalleeId: String? = null

    // UI State — простые var, не remember (мы в Activity, не в Composable)
    private var userId = "user123"
    private var callStatus by mutableStateOf(CallStatus.IDLE)
    private var showIncomingCallDialog by mutableStateOf(false)
    private var incomingSessionId by mutableStateOf("")
    private var incomingCallerId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        if (ApiClient.TEST_MODE) {
            android.util.Log.w("MainActivity", "TEST MODE enabled — using ORDS test_connect")
        }

        apiClient = ApiClient()

        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            P2PAndroidTheme {
                CallScreen()
            }
        }

        requestPermissions()
        registerFcmToken()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.action == "incoming_call") {
            val sessionId = intent.getStringExtra("session_id")
            val callerId = intent.getStringExtra("caller_id")
            if (sessionId != null && callerId != null) {
                showIncomingCallDialog = true
                incomingSessionId = sessionId
                incomingCallerId = callerId
                callStatus = CallStatus.RINGING
            }
        }
    }

    private fun registerFcmToken() {
        if (ApiClient.TEST_MODE) {
            android.util.Log.w("MainActivity", "TEST MODE: skipping FCM token registration")
            return
        }
        lifecycleScope.launch {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    // registerUser: userId, name, fcmToken
                    apiClient.registerUser(userId, userId, token) { success ->
                        android.util.Log.d("MainActivity", "FCM token registered: $success")
                    }
                }
            }
        }
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

    @Composable
    fun CallScreen() {
        var calleeId by remember { mutableStateOf(TextFieldValue("")) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User ID display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your ID: $userId",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Callee ID input
                OutlinedTextField(
                    value = calleeId,
                    onValueChange = { calleeId = it },
                    label = { Text("Enter callee ID") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Call status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (callStatus) {
                            CallStatus.IDLE -> MaterialTheme.colorScheme.secondaryContainer
                            CallStatus.CONNECTING -> Color(0xFFFFEB3B)
                            CallStatus.RINGING -> Color(0xFFFF9800)
                            CallStatus.IN_PROGRESS -> Color(0xFF4CAF50)
                            CallStatus.ENDED -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Text(
                        text = "Status: ${callStatus.name}",
                        modifier = Modifier.padding(12.dp),
                        color = when (callStatus) {
                            CallStatus.IDLE -> MaterialTheme.colorScheme.onSecondaryContainer
                            CallStatus.CONNECTING -> Color.Black
                            CallStatus.RINGING -> Color.White
                            CallStatus.IN_PROGRESS -> Color.White
                            CallStatus.ENDED -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (callStatus == CallStatus.IDLE) {
                                initiateCall(calleeId.text)
                            }
                        },
                        enabled = callStatus == CallStatus.IDLE && calleeId.text.isNotBlank(),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(48.dp),
                            tint = if (callStatus == CallStatus.IDLE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = { hangUp() },
                        enabled = callStatus != CallStatus.IDLE,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang Up",
                            modifier = Modifier.size(48.dp),
                            tint = if (callStatus != CallStatus.IDLE)
                                Color(0xFFE53935)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (callStatus) {
                        CallStatus.IDLE -> "Ready to call"
                        CallStatus.CONNECTING -> "Connecting..."
                        CallStatus.RINGING -> "Incoming call..."
                        CallStatus.IN_PROGRESS -> "In progress..."
                        CallStatus.ENDED -> "Call ended"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Incoming call dialog
        if (showIncomingCallDialog) {
            IncomingCallDialog(
                callerId = incomingCallerId,
                onAccept = { acceptCall(incomingSessionId, incomingCallerId) },
                onReject = { rejectCall(incomingSessionId) }
            )
        }
    }

    @Composable
    fun IncomingCallDialog(
        callerId: String,
        onAccept: () -> Unit,
        onReject: () -> Unit
    ) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Incoming call from $callerId",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(onClick = onReject) {
                            Text("Decline")
                        }
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Accept")
                        }
                    }
                }
            }
        }
    }

    private fun initiateCall(calleeIdText: String) {
        if (calleeIdText.isBlank()) return

        callStatus = CallStatus.CONNECTING

        if (ApiClient.TEST_MODE) {
            // Hardcoded test IDs — bypass real Firebase flow
            val testCallerId = "айдар"
            val testCalleeId = "мама"
            currentCalleeId = testCalleeId
            android.util.Log.d("MainActivity", "TEST MODE: initiateCall caller=$testCallerId callee=$testCalleeId")

            apiClient.testConnect(testCallerId, testCalleeId) { sessionId ->
                runOnUiThread {
                    sessionId?.let {
                        currentSessionId = it
                        Toast.makeText(this, "Test call initiated: $it", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        callStatus = CallStatus.ENDED
                        Toast.makeText(this, "Failed to initiate test call", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            currentCalleeId = calleeIdText

            apiClient.initiateCall(userId, calleeIdText) { sessionId ->
                runOnUiThread {
                    sessionId?.let {
                        currentSessionId = it
                        Toast.makeText(this, "Call initiated: $it", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        callStatus = CallStatus.ENDED
                        Toast.makeText(this, "Failed to initiate call", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun acceptCall(sessionId: String, callerId: String) {
        showIncomingCallDialog = false
        callStatus = CallStatus.IN_PROGRESS
        currentSessionId = sessionId
        currentCallerId = callerId
        Toast.makeText(this, "Call accepted", Toast.LENGTH_SHORT).show()
    }

    private fun rejectCall(sessionId: String) {
        showIncomingCallDialog = false
        callStatus = CallStatus.ENDED
        apiClient.updateCallStatus(sessionId, "rejected") { success ->
            android.util.Log.d("MainActivity", "Reject status: $success")
        }
        Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
    }

    private fun hangUp() {
        currentSessionId?.let { sessionId ->
            apiClient.updateCallStatus(sessionId, "ended") { success ->
                android.util.Log.d("MainActivity", "HangUp status: $success")
            }
        }
        callStatus = CallStatus.ENDED
        webRTCManager?.hangUp()
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
    }
}

enum class CallStatus {
    IDLE, CONNECTING, RINGING, IN_PROGRESS, ENDED
}