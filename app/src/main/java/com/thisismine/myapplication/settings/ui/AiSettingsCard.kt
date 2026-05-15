package com.thisismine.myapplication.settings.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.thisismine.myapplication.settings.AppSettings
import com.thisismine.myapplication.settings.AppSettingsStore
import com.thisismine.myapplication.settings.GeminiApiKeyStore

@Composable
fun AiSettingsCard(
    appSettings: AppSettings,
    sharedPreferences: SharedPreferences,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geminiPrefs = remember(context) {
        context.getSharedPreferences(GeminiApiKeyStore.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var apiKey by remember { mutableStateOf(GeminiApiKeyStore.load(geminiPrefs)) }
    var showApiKey by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val savedApiKey = GeminiApiKeyStore.load(geminiPrefs)
    val geminiConnected = appSettings.geminiEnabled && savedApiKey.isNotBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gemini AI Features",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Sign in with your Google Gemini API account (free tier available at AI Studio).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Gemini Enabled toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Gemini AI",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = if (geminiConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (geminiConnected)
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appSettings.geminiEnabled,
                    onCheckedChange = { checked ->
                        val newSettings = appSettings.copy(geminiEnabled = checked)
                        AppSettingsStore.save(sharedPreferences, newSettings)
                        onSettingsChanged(newSettings)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // API Key input
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key") },
                visualTransformation = if (showApiKey) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showApiKey = !showApiKey }
                    ) {
                        Icon(
                            imageVector = if (showApiKey) 
                                Icons.Filled.Visibility 
                            else 
                                Icons.Filled.VisibilityOff,
                            contentDescription = if (showApiKey) 
                                "Hide API key" 
                            else 
                                "Show API key"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isValidating
            )

            Text(
                text = "Get your free API key from: https://aistudio.google.com/app/apikey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Redact sensitive data toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Redact Sensitive Data",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "VIN, plate number, and personal notes won't be sent to AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appSettings.redactSensitiveDataForAi,
                    onCheckedChange = { checked ->
                        val newSettings = appSettings.copy(redactSensitiveDataForAi = checked)
                        AppSettingsStore.save(sharedPreferences, newSettings)
                        onSettingsChanged(newSettings)
                    }
                )
            }

            // Save/Validate button
            Button(
                onClick = {
                    isValidating = true
                    validationMessage = null
                    // Simple validation: check if key is not empty and reasonable length
                    if (apiKey.isNotBlank() && apiKey.length > 20) {
                        GeminiApiKeyStore.save(geminiPrefs, apiKey)
                        apiKey = GeminiApiKeyStore.load(geminiPrefs)
                        val newSettings = appSettings.copy(geminiEnabled = true)
                        AppSettingsStore.save(sharedPreferences, newSettings)
                        onSettingsChanged(newSettings)
                        validationMessage = "✓ API key saved successfully"
                        isValidating = false
                    } else {
                        validationMessage = "✗ Please provide a valid API key (at least 20 characters)"
                        isValidating = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank() && !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (savedApiKey == apiKey) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save API Key")
            }

            // Validation message
            if (validationMessage != null) {
                Text(
                    text = validationMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (validationMessage!!.startsWith("✓"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Terms
            Text(
                text = "🔒 Privacy: Your API key is stored locally. Data minimization ensures sensitive fields are redacted before sending to Gemini. AI outputs are advisory only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

