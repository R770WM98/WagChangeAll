package com.thisismine.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.thisismine.myapplication.settings.AppSettingsStore
import com.thisismine.myapplication.settings.GeminiApiKeyStore
import com.thisismine.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsPrefs = getSharedPreferences(AppSettingsStore.PREFS_NAME, MODE_PRIVATE)
        val secretPrefs = getSharedPreferences(GeminiApiKeyStore.PREFS_NAME, MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences(LEGACY_PREFS_FILE, MODE_PRIVATE)

        AppSettingsStore.migrateFromLegacyPrefs(legacyPrefs, settingsPrefs)
        GeminiApiKeyStore.migrateFromLegacyPrefs(legacyPrefs, secretPrefs)

        val storedSettings = AppSettingsStore.load(settingsPrefs)

        setContent {
            var settings by remember { mutableStateOf(storedSettings) }
            val systemIsDark = isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = settings.themeMode.resolveDarkTheme(systemIsDark)) {
                WagChangeAllApp(
                    settings = settings,
                    onSettingsChange = { updated ->
                        settings = updated
                        AppSettingsStore.save(settingsPrefs, updated)
                    }
                )
            }
        }
    }

    private companion object {
        const val LEGACY_PREFS_FILE = "wagchange-ui"
    }
}
