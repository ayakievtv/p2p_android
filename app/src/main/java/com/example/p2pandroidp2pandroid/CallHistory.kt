package com.example.p2pandroidp2pandroid

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Manages call history locally.
 */
class CallHistory(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "call_history",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    data class CallRecord(
        val sessionId: String,
        val callerId: String,
        val calleeId: String,
        val status: String,
        val timestamp: Long
    )
    
    fun addCall(sessionId: String, callerId: String, calleeId: String, status: String) {
        val record = CallRecord(
            sessionId = sessionId,
            callerId = callerId,
            calleeId = calleeId,
            status = status,
            timestamp = System.currentTimeMillis()
        )
        
        val history = getHistory().toMutableList()
        history.add(0, record)
        
        // Keep only last 100 calls
        if (history.size > 100) {
            history.removeAt(history.size - 1)
        }
        
        saveHistory(history)
    }
    
    fun getHistory(): List<CallRecord> {
        val json = prefs.getString("history", "[]")
        val type = object : TypeToken<List<CallRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    private fun saveHistory(history: List<CallRecord>) {
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }
    
    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }
}
