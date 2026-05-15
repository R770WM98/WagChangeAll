package com.thisismine.myapplication.settings

import android.content.SharedPreferences
import androidx.core.content.edit

object GeminiApiKeyStore {
    const val PREFS_NAME = "app_gemini_secret"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

    fun load(prefs: SharedPreferences): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun save(prefs: SharedPreferences, apiKey: String) {
        prefs.edit {
            if (apiKey.isBlank()) {
                remove(KEY_GEMINI_API_KEY)
            } else {
                putString(KEY_GEMINI_API_KEY, apiKey)
            }
        }
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit { remove(KEY_GEMINI_API_KEY) }
    }

    fun migrateFromLegacyPrefs(legacyPrefs: SharedPreferences, targetPrefs: SharedPreferences) {
        if (targetPrefs.getString(KEY_GEMINI_API_KEY, null) != null || legacyPrefs.all.isEmpty()) return

        val apiKey = legacyPrefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()
        if (apiKey.isNotBlank()) {
            save(targetPrefs, apiKey)
        }
    }
}


