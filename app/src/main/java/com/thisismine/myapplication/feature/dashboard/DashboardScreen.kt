package com.thisismine.myapplication.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisismine.myapplication.core.ui.AppCardShell
import com.thisismine.myapplication.core.ui.CardHeaderRow
import com.thisismine.myapplication.core.ui.CompactIconAction
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.StatusChip
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.reminders.notifications.AlarmReminderScheduler
import com.thisismine.myapplication.ui.theme.MyApplicationTheme
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    wagChangeRepository: WagChangeRepository,
    onOpenMotorcycles: () -> Unit,
    onOpenOdometer: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenFuel: () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationScheduler = remember(context) { AlarmReminderScheduler(context.applicationContext) }
    val vm: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            repository = wagChangeRepository,
            notificationScheduler = notificationScheduler
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val activeMotorcycle by wagChangeRepository.activeMotorcycleFlow.collectAsStateWithLifecycle()

    LaunchedEffect(activeMotorcycle?.id) {
        vm.refresh()
    }

    val density = LocalCardDensity.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(density.listContentPadding),
        verticalArrangement = Arrangement.spacedBy(density.sectionSpacing)
    ) {
        item {
            ActiveMotorcycleCard(
                name = uiState.activeMotorcycle?.name,
                odometerKm = uiState.activeMotorcycle?.currentOdometerKm,
                motorcycleCount = uiState.motorcycleCount,
                onSwitch = onOpenMotorcycles
            )
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

        item {
            AlertsCard(
                alerts = uiState.alerts,
                onOpenReminders = onOpenReminders,
                onSnoozeReminder = vm::snoozeReminder,
                onMarkDoneReminder = vm::markDoneReminder
            )
        }

        item {
            DueTimelineCard(
                items = uiState.dueTimeline,
                onOpenReminders = onOpenReminders,
                onSnoozeReminder = vm::snoozeReminder,
                onMarkDoneReminder = vm::markDoneReminder
            )
        }

        item {
            QuickActionsCard(
                onOpenMotorcycles = onOpenMotorcycles,
                onOpenOdometer = onOpenOdometer,
                onOpenMaintenance = onOpenMaintenance,
                motorcycleName = uiState.activeMotorcycle?.name ?: "Motorcycle",
                maintenanceEntries = wagChangeRepository.getMaintenanceEntries(),
                partReplacements = wagChangeRepository.getPartReplacements()
            )
        }

        item {
            OdometerSnapshotCard(snapshot = uiState.odometer)
        }

        item {
            FuelAnalyticsCard(snapshot = uiState.fuelAnalytics, onOpenFuel = onOpenFuel)
        }

        item {
            MonthlySpendCard(items = uiState.monthlySpend)
        }

        item {
            UpcomingMaintenanceCard(
                items = uiState.upcomingMaintenance,
                onOpenMaintenance = onOpenMaintenance
            )
        }

        item {
            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.recentActivity.isEmpty()) {
            item {
                AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
                    Text("No activity yet for the active motorcycle.")
                }
            }
        } else {
            items(uiState.recentActivity) { item ->
                AppCardShell(contentPadding = PaddingValues(horizontal = density.cardPadding, vertical = density.cardPadding - 4.dp)) {
                    Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = item.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = item.dateIso.toDisplayDate(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = density.rowSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveMotorcycleCard(
    name: String?,
    odometerKm: Int?,
    motorcycleCount: Int,
    onSwitch: () -> Unit
) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.TwoWheeler, title = "Active Motorcycle")
        Text(
            text = name ?: "No motorcycle selected",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (odometerKm == null) "Add a motorcycle to start tracking." else "Odometer: $odometerKm km",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = "Bikes in garage: $motorcycleCount",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
        )
        Button(onClick = onSwitch) { Text("Open bikes") }
    }
}

@Composable
private fun QuickActionsCard(
    onOpenMotorcycles: () -> Unit,
    onOpenOdometer: () -> Unit,
    onOpenMaintenance: () -> Unit,
    motorcycleName: String = "Motorcycle",
    maintenanceEntries: List<com.thisismine.myapplication.domain.model.MaintenanceEntry> = emptyList(),
    partReplacements: List<com.thisismine.myapplication.domain.model.PartReplacement> = emptyList()
) {
    val context = LocalContext.current
    val density = LocalCardDensity.current
    val showPrintDialog = remember { mutableStateOf(false) }
    val hasBothHistories = maintenanceEntries.isNotEmpty() && partReplacements.isNotEmpty()

    AppCardShell(tonal = true, contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Timeline, title = "Quick Actions")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onOpenOdometer, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Speed, contentDescription = null)
                Text(" Add Reading")
            }
            Button(onClick = onOpenMaintenance, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Build, contentDescription = null)
                Text(" Log Service")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onOpenMotorcycles,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.TwoWheeler, contentDescription = null)
                Text(" Open bikes")
            }
            FilledTonalButton(
                onClick = { showPrintDialog.value = true },
                modifier = Modifier.weight(1f),
                enabled = maintenanceEntries.isNotEmpty() || partReplacements.isNotEmpty()
            ) {
                Icon(Icons.Filled.Print, contentDescription = null)
                Text(" Print History")
            }
        }
    }

    if (showPrintDialog.value) {
        PrintSelectionDialog(
            motorcycleName = motorcycleName,
            maintenanceEntries = maintenanceEntries,
            partReplacements = partReplacements,
            hasBothHistories = hasBothHistories,
            onDismiss = { showPrintDialog.value = false },
            onPrintService = {
                printServiceHistory(context, motorcycleName, maintenanceEntries)
                showPrintDialog.value = false
            },
            onPrintParts = {
                printPartHistory(context, motorcycleName, partReplacements)
                showPrintDialog.value = false
            },
            onPrintCombined = {
                printCombinedHistory(context, motorcycleName, maintenanceEntries, partReplacements)
                showPrintDialog.value = false
            }
        )
    }
}

@Composable
private fun OdometerSnapshotCard(snapshot: OdometerSnapshot) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Speed, title = "Odometer Snapshot")
        Text(text = "Current: ${snapshot.currentKm} km", modifier = Modifier.padding(top = 6.dp))
        Text(text = "Last 30 days: ${snapshot.distanceLast30DaysKm} km", modifier = Modifier.padding(top = 2.dp))
        Text(text = "Readings: ${snapshot.readingCount}", modifier = Modifier.padding(top = 2.dp))
        Text(
            text = "Last update: ${snapshot.lastReadingDateIso?.toDisplayDate() ?: "No reading yet"}",
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun FuelAnalyticsCard(
    snapshot: FuelAnalyticsSnapshot,
    onOpenFuel: () -> Unit
) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.LocalGasStation, title = "Fuel Analytics")
        if (snapshot.entryCount == 0) {
            Text(
                text = "No fuel entries yet. Start tracking to see analytics.",
                modifier = Modifier.padding(top = density.rowSpacing)
            )
        } else {
            Text(text = "Entries: ${snapshot.entryCount}", modifier = Modifier.padding(top = 6.dp))
            Text(
                text = "Average efficiency: ${snapshot.averageKmPerLiter?.let { "%.2f km/L".format(it) } ?: "N/A"}",
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "Cost per km: ${snapshot.costPerKm?.let { "PHP %.2f/km".format(it) } ?: "N/A"}",
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "Last entry: ${snapshot.lastEntryDateIso?.toDisplayDate() ?: "N/A"}",
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Button(
            onClick = onOpenFuel,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Filled.LocalGasStation, contentDescription = null)
            Text(" View Fuel Analytics")
        }
    }
}

@Composable
private fun AlertsCard(
    alerts: List<DashboardAlert>,
    onOpenReminders: () -> Unit,
    onSnoozeReminder: (String) -> Boolean,
    onMarkDoneReminder: (String) -> Boolean
) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Notifications, title = "Urgent Alerts")
        if (alerts.isEmpty()) {
            Row(
                modifier = Modifier.padding(top = density.rowSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(text = "On track", tone = StatusTone.Positive)
                Text("No urgent items. You're up to date.")
            }
        } else {
            alerts.forEach { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = density.rowSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip(
                        text = alert.severity.displayLabel(),
                        tone = alert.severity.toStatusTone()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = alert.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = alert.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    if (alert.reminderId != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            CompactIconAction(
                                icon = Icons.Filled.Snooze,
                                contentDescription = "Snooze reminder",
                                onClick = { onSnoozeReminder(alert.reminderId) }
                            )
                            CompactIconAction(
                                icon = Icons.Filled.CheckCircle,
                                contentDescription = "Mark reminder done",
                                onClick = { onMarkDoneReminder(alert.reminderId) }
                            )
                        }
                    }
                }
            }
        }
        TextButton(onClick = onOpenReminders, modifier = Modifier.padding(top = 4.dp)) {
            Text("Open reminders")
        }
    }
}

@Composable
private fun DueTimelineCard(
    items: List<DueTimelineItem>,
    onOpenReminders: () -> Unit,
    onSnoozeReminder: (String) -> Boolean,
    onMarkDoneReminder: (String) -> Boolean
) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Timeline, title = "Due Timeline")
        if (items.isEmpty()) {
            Text(
                text = "No scheduled reminders yet.",
                modifier = Modifier.padding(top = density.rowSpacing)
            )
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = density.rowSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusChip(text = item.severity.displayLabel(), tone = item.severity.toStatusTone())
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        CompactIconAction(
                            icon = Icons.Filled.Snooze,
                            contentDescription = "Snooze reminder",
                            onClick = { onSnoozeReminder(item.reminderId) }
                        )
                        CompactIconAction(
                            icon = Icons.Filled.CheckCircle,
                            contentDescription = "Mark reminder done",
                            onClick = { onMarkDoneReminder(item.reminderId) }
                        )
                    }
                }
            }
        }
        TextButton(onClick = onOpenReminders, modifier = Modifier.padding(top = 4.dp)) {
            Text("Open reminders")
        }
    }
}

@Composable
private fun MonthlySpendCard(items: List<MonthlySpendItem>) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Build, title = "Monthly Spend")
        if (items.isEmpty()) {
            Text(
                text = "No spend data for recent months yet.",
                modifier = Modifier.padding(top = density.rowSpacing)
            )
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = density.rowSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.monthLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Maintenance PHP ${"%.0f".format(item.maintenanceTotalPhp)} • Fuel PHP ${"%.0f".format(item.fuelTotalPhp)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "PHP ${"%.0f".format(item.totalPhp)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingMaintenanceCard(
    items: List<UpcomingMaintenanceItem>,
    onOpenMaintenance: () -> Unit
) {
    val density = LocalCardDensity.current
    AppCardShell(contentPadding = PaddingValues(density.cardPadding)) {
        CardHeaderRow(icon = Icons.Filled.Build, title = "Upcoming Maintenance")
        if (items.isEmpty()) {
            Text(
                text = "No reminders configured yet.",
                modifier = Modifier.padding(top = density.rowSpacing)
            )
        } else {
            items.forEach { item ->
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = density.rowSpacing)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    dueTokens(item.dueText).forEach { token ->
                        StatusChip(
                            text = token,
                            tone = token.severityFromDueText().toStatusTone()
                        )
                    }
                }
            }
        }
        TextButton(onClick = onOpenMaintenance, modifier = Modifier.padding(top = 4.dp)) {
            Text("Open maintenance log")
        }
    }
}

private fun dueTokens(dueText: String): List<String> =
    dueText.split(',').map { it.trim() }.filter { it.isNotBlank() }

private fun String.severityFromDueText(): AlertSeverity = when {
    contains("overdue", ignoreCase = true) -> AlertSeverity.Overdue
    contains("due now", ignoreCase = true) || contains("due today", ignoreCase = true) -> AlertSeverity.DueSoon
    else -> AlertSeverity.Info
}

private fun AlertSeverity.displayLabel(): String = when (this) {
    AlertSeverity.Overdue -> "Overdue"
    AlertSeverity.DueSoon -> "Due soon"
    AlertSeverity.Info -> "Info"
}

private fun String.toDisplayDate(): String {
    val parsed = toLocalDateOrNull() ?: return this
    return parsed.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
}

private fun AlertSeverity.toStatusTone(): StatusTone = when (this) {
    AlertSeverity.Overdue -> StatusTone.Critical
    AlertSeverity.DueSoon -> StatusTone.Warning
    AlertSeverity.Info -> StatusTone.Info
}

@Composable
private fun PrintSelectionDialog(
    motorcycleName: String,
    maintenanceEntries: List<com.thisismine.myapplication.domain.model.MaintenanceEntry>,
    partReplacements: List<com.thisismine.myapplication.domain.model.PartReplacement>,
    hasBothHistories: Boolean,
    onDismiss: () -> Unit,
    onPrintService: () -> Unit,
    onPrintParts: () -> Unit,
    onPrintCombined: () -> Unit
) {
    if (hasBothHistories) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select History to Print") },
            text = { Text("Choose what you want to print for $motorcycleName") },
            confirmButton = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onPrintCombined) {
                        Text("Combined History")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onPrintService) {
                        Text("Service History")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = onPrintParts) {
                        Text("Parts History")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    } else if (maintenanceEntries.isNotEmpty()) {
        // Only service history available
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Print Service History?") },
            text = { Text("This will print the complete service history for $motorcycleName") },
            confirmButton = {
                Button(onClick = onPrintService) {
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    } else if (partReplacements.isNotEmpty()) {
        // Only parts history available
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Print Parts History?") },
            text = { Text("This will print the parts replacement history for $motorcycleName") },
            confirmButton = {
                Button(onClick = onPrintParts) {
                    Text("Print")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Dashboard - Active Bike Card", showBackground = true)
@Composable
private fun ActiveMotorcycleCardPreview() {
    MyApplicationTheme {
        ActiveMotorcycleCard(
            name = "Daily Ride",
            odometerKm = 21450,
            motorcycleCount = 3,
            onSwitch = {}
        )
    }
}

@Preview(name = "Dashboard - Alerts", showBackground = true)
@Composable
private fun AlertsCardPreview() {
    MyApplicationTheme {
        AlertsCard(
            alerts = listOf(
                DashboardAlert("r1", "Engine Oil", "overdue by 120 km", AlertSeverity.Overdue),
                DashboardAlert("r2", "Brake Check", "due in 3 days", AlertSeverity.DueSoon)
            ),
            onOpenReminders = {},
            onSnoozeReminder = { true },
            onMarkDoneReminder = { true }
        )
    }
}

@Preview(name = "Dashboard - Quick Actions", showBackground = true)
@Composable
private fun QuickActionsCardPreview() {
    MyApplicationTheme {
        QuickActionsCard(
            onOpenMotorcycles = {},
            onOpenOdometer = {},
            onOpenMaintenance = {},
            motorcycleName = "Test Bike",
            maintenanceEntries = emptyList(),
            partReplacements = emptyList()
        )
    }
}

@Preview(name = "Dashboard - Odometer Snapshot", showBackground = true)
@Composable
private fun OdometerSnapshotCardPreview() {
    MyApplicationTheme {
        OdometerSnapshotCard(
            snapshot = OdometerSnapshot(
                currentKm = 21450,
                lastReadingDateIso = "2026-04-04",
                distanceLast30DaysKm = 560,
                readingCount = 18
            )
        )
    }
}

@Preview(name = "Dashboard - Upcoming Maintenance", showBackground = true)
@Composable
private fun UpcomingMaintenanceCardPreview() {
    MyApplicationTheme {
        UpcomingMaintenanceCard(
            items = listOf(
                UpcomingMaintenanceItem("CVT Cleaning", "due in 120 km, due in 5 days"),
                UpcomingMaintenanceItem("Chain Service", "overdue by 20 km")
            ),
            onOpenMaintenance = {}
        )
    }
}

@Preview(name = "Dashboard - Due Timeline", showBackground = true)
@Composable
private fun DueTimelineCardPreview() {
    MyApplicationTheme {
        DueTimelineCard(
            items = listOf(
                DueTimelineItem("r1", "Oil Change", "overdue by 120 km", AlertSeverity.Overdue),
                DueTimelineItem("r2", "Brake Check", "due in 3 days", AlertSeverity.DueSoon)
            ),
            onOpenReminders = {},
            onSnoozeReminder = { true },
            onMarkDoneReminder = { true }
        )
    }
}

@Preview(name = "Dashboard - Monthly Spend", showBackground = true)
@Composable
private fun MonthlySpendCardPreview() {
    MyApplicationTheme {
        MonthlySpendCard(
            items = listOf(
                MonthlySpendItem("Mar 2026", maintenanceTotalPhp = 780.0, fuelTotalPhp = 320.0, totalPhp = 1100.0),
                MonthlySpendItem("Feb 2026", maintenanceTotalPhp = 450.0, fuelTotalPhp = 210.0, totalPhp = 660.0)
            )
        )
    }
}

