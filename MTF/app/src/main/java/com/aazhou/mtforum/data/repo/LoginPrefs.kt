package com.aazhou.mtforum.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey







class LoginPrefs(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "mtf_login_prefs_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        
        android.util.Log.e("LoginPrefs", "EncryptedSharedPreferences 初始化失败，降级为明文存储: ${e.message}")
        context.getSharedPreferences("mtf_login_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString("last_username", username)
            .putString("last_password", password)
            .apply()
    }

    fun loadUsername(): String = prefs.getString("last_username", "") ?: ""
    fun loadPassword(): String = prefs.getString("last_password", "") ?: ""

    fun clear() {
        prefs.edit().remove("last_username").remove("last_password").apply()
    }
}
