package com.thisismine.myapplication.feature.mechanic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.thisismine.myapplication.core.ui.LocalCardDensity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MechanicSummaryScreen(
    viewModel: MechanicSummaryViewModel = viewModel()
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
            ScrollableTextWindow(
                text = content,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScrollableTextWindow(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    showFullScreenButton: Boolean = true
) {
    val scrollState = rememberScrollState()
    val density = LocalCardDensity.current
    val maxHeight = if (density.cardPadding <= 12.dp) 120.dp else 160.dp
    var showFullScreen by remember { mutableStateOf(false) }
    var hoverIconVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp))
            .pointerInteropFilter { ev ->
                when (ev.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> hoverIconVisible = true
                    MotionEvent.ACTION_HOVER_EXIT -> hoverIconVisible = false
                }
                false
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    hoverIconVisible = true
                    scope.launch { delay(3000); hoverIconVisible = false }
                })
            },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val iconAreaPadding =  if (hoverIconVisible) 36.dp else 12.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 12.dp, end = 12.dp, top = iconAreaPadding, bottom = 12.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }

            val fadeHeight = 20.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        )
                    )
            )

            if (hoverIconVisible) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = { showFullScreen = true },
                        modifier = Modifier
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open full screen",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showFullScreen) {
        Dialog(onDismissRequest = { showFullScreen = false }) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Full output", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showFullScreen = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider()
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(text = text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    value: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = {},
        label = {
            Text("Confidence: $value", style = MaterialTheme.typography.labelSmall)
        },
        modifier = modifier
    )
}
