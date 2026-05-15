package com.thisismine.myapplication.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thisismine.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Text

@Composable
fun AppCardShell(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    fullWidth: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val resolvedModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    Card(
        modifier = resolvedModifier,
        colors = CardDefaults.cardColors(
            containerColor = if (tonal) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Preview(name = "App Card Shell - Default", showBackground = true)
@Composable
private fun AppCardShellPreview() {
    MyApplicationTheme {
        AppCardShell {
            Text("Default app card shell")
        }
    }
}

@Preview(name = "App Card Shell - Tonal", showBackground = true)
@Composable
private fun AppCardShellTonalPreview() {
    MyApplicationTheme {
        AppCardShell(tonal = true) {
            Text("Tonal app card shell")
        }
    }
}
