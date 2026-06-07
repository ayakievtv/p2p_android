package com.example.p2pandroidp2pandroid

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * Telecom ConnectionService for system-level call integration.
 * Allows calls to appear in the system dialer and respect system call UI.
 */
class CallConnectionService : ConnectionService() {
    companion object {
        private const val TAG = "CallConnectionService"
        var activeConnection: CallConnection? = null
    }

    // Исходящий звонок
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Creating outgoing connection: ${request.address}")
        return createConnection(request.address)
    }

    // Входящий звонок
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        Log.d(TAG, "Creating incoming connection: ${request.address}")
        val connection = createConnection(request.address)
        connection.setRinging()
        return connection
    }

    private fun createConnection(address: Uri?): CallConnection {
        val connection = CallConnection().apply {
            setAddress(address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            setInitialized()
        }
        activeConnection = connection
        return connection
    }
}

/**
 * Представляет один активный звонок.
 */
class CallConnection : Connection() {
    companion object {
        private const val TAG = "CallConnection"
    }

    override fun onAnswer() {
        super.onAnswer()
        Log.d(TAG, "Call answered")
        setActive()
    }

    override fun onReject() {
        super.onReject()
        Log.d(TAG, "Call rejected")
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.d(TAG, "Call disconnected")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onHold() {
        super.onHold()
        Log.d(TAG, "Call on hold")
        setOnHold()
    }

    override fun onUnhold() {
        super.onUnhold()
        Log.d(TAG, "Call resumed")
        setActive()
    }
}