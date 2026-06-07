package com.example.p2pandroidp2pandroid

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper for managing user profile data.
 * Stores user ID, name, and settings locally.
 */
class UserProfileHelper(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_profile",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
    
    fun getUserId(): String {
        return prefs.getString(KEY_USER_ID, "user_${System.currentTimeMillis()}") ?: run {
            generateAndSaveUserId()
        }
    }
  
  
    private fun generateAndSaveUserId(): String {
        val userId = "user_${System.currentTimeMillis()}"
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        return userId
    }
  
  
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }
    
  
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
  
  
    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }
  
    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }
  
    
    fun clearProfile() {
        prefs.edit().clear().apply()
    }
}
