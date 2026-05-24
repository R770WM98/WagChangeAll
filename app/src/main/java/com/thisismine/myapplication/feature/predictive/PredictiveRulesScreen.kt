package com.thisismine.myapplication.feature.predictive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import com.thisismine.myapplication.core.ui.LocalCardDensity
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PredictiveRulesScreen(
    viewModel: PredictiveRulesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.suggestions.isEmpty() && uiState.error == null) {
            viewModel.generateRules()
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
                    text = "Predictive Rules",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "AI-suggested maintenance intervals",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.generateRules(forceRefresh = true) },
                enabled = !uiState.isLoading
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh rules"
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

        // Success message state
        if (uiState.saveMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = uiState.saveMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
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

        // Suggestions list
        if (uiState.suggestions.isNotEmpty()) {
            Text(
                text = "${uiState.suggestions.size} Suggestions",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            uiState.suggestions.forEach { suggestion ->
                RuleSuggestionCard(
                    suggestion = suggestion,
                    onApprove = { viewModel.approveSuggestion(suggestion) },
                    onReject = { viewModel.rejectSuggestion(suggestion) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Approved summary
            if (uiState.approvedSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Approved Rules (${uiState.approvedSuggestions.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.approvedSuggestions.forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.component,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                    Text(
                                        text = rule.suggestedInterval,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.saveApprovedRules() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("Save Approved Rules")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer note
        Text(
            text = "⚠️ These suggestions are AI-generated and based on your service history. Always verify with manufacturer recommendations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun RuleSuggestionCard(
    suggestion: com.thisismine.myapplication.ai.PredictiveRulesSuggestion,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = suggestion.component,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            suggestion.confidence,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            Text(
                text = "Interval: ${suggestion.suggestedInterval}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            ScrollableTextWindow(
                text = "Rationale: ${suggestion.rationale}",
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            ScrollableTextWindow(
                text = "Risk: ${suggestion.riskIfSkipped}",
                textColor = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
                Button(onClick = onApprove) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
            }
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
    // Determine max height from density mode: smaller for compact
    val maxHeight = if (density.cardPadding <= 12.dp) 120.dp else 160.dp
    var showFullScreen by remember { mutableStateOf(false) }
    var hoverIconVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp))
            // show hover icon on pointer enter/exit (desktop) and on long-press (touch)
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
            val iconAreaPadding = if (showFullScreenButton) 36.dp else 12.dp
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

            // top/bottom subtle gradient indicators
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

            // show icon only when hovered (desktop) or revealed by long-press (touch)
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

