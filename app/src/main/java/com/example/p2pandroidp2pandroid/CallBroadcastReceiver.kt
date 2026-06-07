package com.example.p2pandroidp2pandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver for system call events.
 * Handles headset button clicks and other broadcast events.
 */
class CallBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallBroadcastReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                Log.d(TAG, "Headset plugged")
                // Could toggle speakerphone or pause video
            }
        
            "com.example.p2papp.action.END_CALL" -> {
                endCall(context)
            }
        }
    }
  
    private fun endCall(context: Context) {
        val service = Intent(context, CallService::class.java)
        context.stopService(service)
    }
}
