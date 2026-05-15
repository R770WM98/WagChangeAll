package com.thisismine.myapplication.feature.mechanic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MechanicSummaryScreen(
    viewModel: MechanicSummaryViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.summary == null && uiState.error == null) {
            viewModel.generateSummary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mechanic Summary",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "AI-powered service history for mechanics",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.generateSummary(forceRefresh = true) },
                enabled = !uiState.isLoading
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh summary"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Error state
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Summary cards
        if (uiState.summary != null) {
            val summary = uiState.summary!!

            // Confidence badge
            SummaryBadge(
                label = "Confidence",
                value = summary.confidence,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Summary section
            SummaryCard(
                title = "Overview",
                content = summary.summary
            )

            // Recent work
            SummaryCard(
                title = "Recent Work",
                content = summary.recentWork
            )

            // Current concerns
            SummaryCard(
                title = "Current Concerns",
                content = summary.currentConcerns
            )

            // Recommended checks
            SummaryCard(
                title = "Recommended Checks",
                content = summary.recommendedChecks
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer note
            Text(
                text = "⚠️ This summary is AI-generated and advisory only. Always consult with a qualified mechanic for diagnosis and repairs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = {},
        label = {
            Text("$label: $value", style = MaterialTheme.typography.labelSmall)
        },
        modifier = modifier
    )
}
