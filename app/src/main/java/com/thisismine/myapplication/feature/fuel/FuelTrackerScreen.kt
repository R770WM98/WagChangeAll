package com.thisismine.myapplication.feature.fuel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisismine.myapplication.core.ui.AppCardShell
import com.thisismine.myapplication.core.ui.CardHeaderRow
import com.thisismine.myapplication.core.ui.CompactIconAction
import com.thisismine.myapplication.core.ui.DatePickerField
import com.thisismine.myapplication.core.ui.FormDialogShell
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.LocalMotionScheme
import com.thisismine.myapplication.core.ui.StatusChip
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.core.ui.animatedProgressValue
import com.thisismine.myapplication.core.ui.itemPlacementSpec
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.fuel.data.WagChangeFuelAnalyticsRepository
import java.time.LocalDate
import java.time.YearMonth

data class FuelEntryDraft(
    val editingId: String? = null,
    val dateIso: String = "",
    val liters: String = "",
    val costPhp: String = "",
    val distanceKm: String = ""
)

@Composable
fun FuelTrackerScreen(
    wagChangeRepository: WagChangeRepository
) {
    val vm: FuelAnalyticsViewModel = viewModel(
        factory = FuelAnalyticsViewModelFactory(
            repository = WagChangeFuelAnalyticsRepository(wagChangeRepository)
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val density = LocalCardDensity.current
    val motion = LocalMotionScheme.current

    var draft by remember { mutableStateOf(FuelEntryDraft()) }
    var showForm by remember { mutableStateOf(false) }
    var filterMonths by remember { mutableStateOf(3) } // 1, 3, 6, or 12 months

    fun openAdd() {
        draft = FuelEntryDraft(
            dateIso = LocalDate.now().toString(),
            distanceKm = uiState.currentOdometerKm.toString()
        )
        showForm = true
    }

    fun closeForm() {
        draft = FuelEntryDraft()
        showForm = false
    }

    fun duplicateLastEntry() {
        if (uiState.entries.isEmpty()) return
        draft = FuelEntryDraft(
            dateIso = LocalDate.now().toString(),
            distanceKm = uiState.currentOdometerKm.toString()
        )
        showForm = true
    }

    fun getFilteredEntries() = uiState.entries.filter { entry ->
        val entryMonth = YearMonth.from(LocalDate.parse(entry.dateIso))
        val cutoffMonth = YearMonth.now().minusMonths((filterMonths - 1).toLong())
        !entryMonth.isBefore(cutoffMonth)
    }

    fun computeAverageEfficiency(entries: List<FuelEntryItemUi>): Double? {
        val validEntries = entries.filter { it.liters > 0.0 && it.distanceKm > 0.0 }
        return if (validEntries.isNotEmpty()) {
            val totalDistance = validEntries.sumOf { it.distanceKm }
            val totalLiters = validEntries.sumOf { it.liters }
            if (totalLiters > 0.0) totalDistance / totalLiters else null
        } else null
    }

    fun checkEfficiencyAnomaly(efficiency: Double?, averageEfficiency: Double?): String? {
        if (efficiency == null || averageEfficiency == null) return null
        val percentDiff = ((averageEfficiency - efficiency) / averageEfficiency) * 100
        return when {
            percentDiff > 15 -> "⚠ Low efficiency (15%+ below average)"
            efficiency < 8.0 -> "⚠ Unusually low efficiency for this bike"
            else -> null
        }
    }

    val filteredEntries = getFilteredEntries()
    val averageEfficiency = computeAverageEfficiency(filteredEntries)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(density.listContentPadding),
        verticalArrangement = Arrangement.spacedBy(density.sectionSpacing)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Fuel tracker", style = MaterialTheme.typography.titleMedium)
                Button(onClick = ::openAdd) {
                    androidx.compose.material3.Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Add entry")
                }
            }
        }

        if (uiState.message != null || uiState.error != null) {
            item {
                AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                    Text(
                        text = uiState.error ?: uiState.message.orEmpty(),
                        color = if (uiState.error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (uiState.entries.isNotEmpty()) {
            item {
                val lastEntry = uiState.entries.first()
                AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                    CardHeaderRow(
                        icon = Icons.Filled.LocalGasStation,
                        title = "Last fill-up: ${lastEntry.dateIso}",
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CompactIconAction(
                                    icon = Icons.Filled.ContentCopy,
                                    contentDescription = "Duplicate last entry",
                                    onClick = ::duplicateLastEntry
                                )
                            }
                        }
                    )
                    Text("Liters: ${"%.2f".format(lastEntry.liters)}", modifier = Modifier.padding(top = 4.dp))
                    Text("Distance: ${"%.1f".format(lastEntry.distanceKm)} km", modifier = Modifier.padding(top = 2.dp))
                    Text("Cost: PHP ${"%.2f".format(lastEntry.costPhp)}", modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "Efficiency: ${lastEntry.efficiencyKmPerLiter?.let { "%.2f km/L".format(it) } ?: "N/A"}",
                        modifier = Modifier.padding(top = 2.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                CardHeaderRow(icon = Icons.Filled.LocalGasStation, title = "Fuel overview")
                Text("Entries: ${uiState.entryCount}", modifier = Modifier.padding(top = 6.dp))
                Text(
                    "Average efficiency: ${uiState.averageKmPerLiter?.let { "%.2f km/L".format(it) } ?: "N/A"}",
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "Cost per km: ${uiState.costPerKm?.let { "PHP %.2f/km".format(it) } ?: "N/A"}",
                    modifier = Modifier.padding(top = 2.dp)
                )
                AnimatedContent(
                    targetState = filterMonths,
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(tween(motion.baseMillis)) togetherWith
                            androidx.compose.animation.fadeOut(tween(motion.fastMillis))
                    },
                    label = "fuel-filter-window"
                ) { months ->
                    Text(
                        text = "Showing last ${months} month${if (months == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 3, 6, 12).forEach { months ->
                    OutlinedButton(
                        onClick = { filterMonths = months },
                        modifier = Modifier.weight(1f),
                        colors = if (filterMonths == months) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("${months}M", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                CardHeaderRow(icon = Icons.Filled.Speed, title = "Rolling efficiency (last 5)")
                if (uiState.rollingEfficiencyKmPerLiter.isEmpty()) {
                    Text("Add fuel entries to see rolling efficiency trend.")
                } else {
                    val maxEfficiency = uiState.rollingEfficiencyKmPerLiter.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
                    uiState.rollingEfficiencyKmPerLiter.forEachIndexed { index, value ->
                        LabeledProgressRow(
                            label = if (index == 0) "Most recent" else "Recent #${index + 1}",
                            valueText = "${"%.2f".format(value)} km/L",
                            progress = (value / maxEfficiency).toFloat().coerceIn(0f, 1f)
                        )
                    }
                }
            }
        }

        item {
            Text("Monthly fuel spend", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.monthlyFuelSpend.isEmpty()) {
            item {
                AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                    CardHeaderRow(icon = Icons.AutoMirrored.Filled.TrendingUp, title = "Trend")
                    Text("No fuel entries to show spend trends.")
                }
            }
        } else {
            val maxSpend = uiState.monthlyFuelSpend.maxOf { it.totalCostPhp }.takeIf { it > 0.0 } ?: 1.0
            items(uiState.monthlyFuelSpend, key = { it.monthLabel }) { item ->
                AppCardShell(
                    contentPadding = PaddingValues(density.cardPadding)
                ) {
                    Text(item.monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LabeledProgressRow(
                        label = "Monthly spend",
                        valueText = "PHP ${"%.0f".format(item.totalCostPhp)}",
                        progress = (item.totalCostPhp / maxSpend).toFloat().coerceIn(0f, 1f),
                        hint = "Higher this month"
                    )
                }
            }
        }

        item {
            Text("Per-bike comparison", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.perBikeComparison.isEmpty()) {
            item { Text("No per-bike data to compare yet.") }
        } else {
            val maxBikeEfficiency = uiState.perBikeComparison.mapNotNull { it.averageKmPerLiter }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val maxBikeCostPerKm = uiState.perBikeComparison.mapNotNull { it.costPerKm }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
            items(uiState.perBikeComparison, key = { it.motorcycleId }) { bike ->
                AppCardShell(
                    contentPadding = PaddingValues(density.cardPadding)
                ) {
                    Text(bike.motorcycleName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Entries: ${bike.entryCount}", modifier = Modifier.padding(top = 2.dp))
                    LabeledProgressRow(
                        label = "Avg km/L",
                        valueText = bike.averageKmPerLiter?.let { "%.2f".format(it) } ?: "N/A",
                        progress = ((bike.averageKmPerLiter ?: 0.0) / maxBikeEfficiency).toFloat().coerceIn(0f, 1f),
                        hint = "Higher is more efficient"
                    )
                    LabeledProgressRow(
                        label = "Cost/km",
                        valueText = bike.costPerKm?.let { "PHP %.2f".format(it) } ?: "N/A",
                        progress = ((bike.costPerKm ?: 0.0) / maxBikeCostPerKm).toFloat().coerceIn(0f, 1f),
                        invert = true,
                        hint = "Lower is cheaper"
                    )
                }
            }
        }

        item {
            Text("Fuel entries", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.entries.isEmpty()) {
            item { Text("No fuel entries recorded yet.") }
        } else {
            items(uiState.entries, key = { it.id }) { entry ->
                val anomaly = checkEfficiencyAnomaly(entry.efficiencyKmPerLiter, averageEfficiency)
                AppCardShell(
                    contentPadding = PaddingValues(density.cardPadding),
                    tonal = anomaly != null
                ) {
                    CardHeaderRow(
                        icon = if (anomaly != null) Icons.Filled.Warning else Icons.Filled.LocalGasStation,
                        title = entry.dateIso,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (anomaly != null) {
                                    StatusChip(text = "Low efficiency", tone = StatusTone.Warning)
                                }
                                CompactIconAction(
                                    icon = Icons.Filled.Edit,
                                    contentDescription = "Edit fuel entry",
                                    onClick = {
                                        draft = FuelEntryDraft(
                                            editingId = entry.id,
                                            dateIso = entry.dateIso,
                                            liters = entry.liters.toString(),
                                            costPhp = entry.costPhp.toString(),
                                            distanceKm = entry.distanceKm.toString()
                                        )
                                        showForm = true
                                    }
                                )
                                CompactIconAction(
                                    icon = Icons.Filled.Delete,
                                    contentDescription = "Delete fuel entry",
                                    onClick = { vm.requestDeleteFuelEntry(entry.id) }
                                )
                            }
                        }
                    )
                    Text("Liters: ${"%.2f".format(entry.liters)}", modifier = Modifier.padding(top = 4.dp))
                    Text("Distance: ${"%.1f".format(entry.distanceKm)} km", modifier = Modifier.padding(top = 2.dp))
                    Text("Cost: PHP ${"%.2f".format(entry.costPhp)}", modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "Efficiency: ${entry.efficiencyKmPerLiter?.let { "%.2f km/L".format(it) } ?: "N/A"}",
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (anomaly != null) {
                        Text(
                            anomaly,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        FormDialogShell(
            title = if (draft.editingId == null) "Add fuel entry" else "Edit fuel entry",
            confirmText = if (draft.editingId == null) "Add" else "Update",
            onConfirm = {
                val ok = vm.saveFuelEntry(
                    editingId = draft.editingId,
                    dateIso = draft.dateIso,
                    litersInput = draft.liters,
                    costInput = draft.costPhp,
                    distanceInput = draft.distanceKm
                )
                if (ok) closeForm()
            },
            onDismiss = ::closeForm,
            error = uiState.error,
            message = uiState.message
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    value = draft.dateIso,
                    onValueChange = { draft = draft.copy(dateIso = it) },
                    label = "Date",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.liters,
                    onValueChange = { draft = draft.copy(liters = it) },
                    label = { Text("Liters") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.distanceKm,
                    onValueChange = { draft = draft.copy(distanceKm = it) },
                    label = { Text("Distance km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.costPhp,
                    onValueChange = { draft = draft.copy(costPhp = it) },
                    label = { Text("Cost PHP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val litersNum = draft.liters.toDoubleOrNull()
                val distanceNum = draft.distanceKm.toDoubleOrNull()
                if (litersNum != null && distanceNum != null && litersNum > 0.0) {
                    val calculatedEfficiency = distanceNum / litersNum
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculated efficiency:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${"%.2f".format(calculatedEfficiency)} km/L",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (uiState.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDeleteFuelEntry,
            title = { Text("Delete fuel entry?") },
            text = { Text("This fuel entry will be removed.") },
            confirmButton = { TextButton(onClick = vm::confirmDeleteFuelEntry) { Text("Delete") } },
            dismissButton = { TextButton(onClick = vm::cancelDeleteFuelEntry) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LabeledProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    invert: Boolean = false,
    hint: String? = null
) {
    val displayProgress = if (invert) 1f - progress else progress
    val animatedProgress = animatedProgressValue(displayProgress)
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(valueText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, style = MaterialTheme.typography.labelSmall)
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}
