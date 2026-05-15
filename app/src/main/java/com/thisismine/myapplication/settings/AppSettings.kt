package com.thisismine.myapplication.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.thisismine.myapplication.core.ui.CardDensityMode
import com.thisismine.myapplication.ui.theme.ThemeMode

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val densityMode: CardDensityMode = CardDensityMode.Comfortable,
    val showActiveBikeBadge: Boolean = true,
    val reduceMotion: Boolean = false,
    val geminiEnabled: Boolean = false,
    val redactSensitiveDataForAi: Boolean = true
)

object AppSettingsStore {
    const val PREFS_NAME = "app_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DENSITY_MODE = "density_mode"
    private const val KEY_SHOW_ACTIVE_BADGE = "show_active_badge"
    private const val KEY_REDUCE_MOTION = "reduce_motion"
    private const val KEY_GEMINI_ENABLED = "gemini_enabled"
    private const val KEY_REDACT_SENSITIVE = "redact_sensitive_for_ai"

    fun load(prefs: SharedPreferences): AppSettings {
        val themeMode = ThemeMode.fromStored(prefs.getString(KEY_THEME_MODE, null))
        val densityMode = runCatching {
            CardDensityMode.valueOf(prefs.getString(KEY_DENSITY_MODE, null) ?: "")
        }.getOrDefault(CardDensityMode.Comfortable)
        val showActiveBikeBadge = prefs.getBoolean(KEY_SHOW_ACTIVE_BADGE, true)
        val reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        val geminiEnabled = prefs.getBoolean(KEY_GEMINI_ENABLED, false)
        val redactSensitive = prefs.getBoolean(KEY_REDACT_SENSITIVE, true)
        return AppSettings(
            themeMode = themeMode,
            densityMode = densityMode,
            showActiveBikeBadge = showActiveBikeBadge,
            reduceMotion = reduceMotion,
            geminiEnabled = geminiEnabled,
            redactSensitiveDataForAi = redactSensitive
        )
    }

    fun save(prefs: SharedPreferences, settings: AppSettings) {
        prefs.edit {
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putString(KEY_DENSITY_MODE, settings.densityMode.name)
            putBoolean(KEY_SHOW_ACTIVE_BADGE, settings.showActiveBikeBadge)
            putBoolean(KEY_REDUCE_MOTION, settings.reduceMotion)
            putBoolean(KEY_GEMINI_ENABLED, settings.geminiEnabled)
            putBoolean(KEY_REDACT_SENSITIVE, settings.redactSensitiveDataForAi)
        }
    }

    fun migrateFromLegacyPrefs(legacyPrefs: SharedPreferences, targetPrefs: SharedPreferences) {
        if (targetPrefs.all.isNotEmpty() || legacyPrefs.all.isEmpty()) return

        save(targetPrefs, load(legacyPrefs))
    }
}
