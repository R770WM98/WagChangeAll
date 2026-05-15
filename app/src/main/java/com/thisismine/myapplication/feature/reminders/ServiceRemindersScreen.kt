package com.thisismine.myapplication.feature.reminders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.thisismine.myapplication.core.ui.AppCardShell
import com.thisismine.myapplication.core.ui.CardHeaderRow
import com.thisismine.myapplication.core.ui.CompactIconAction
import com.thisismine.myapplication.core.ui.DatePickerField
import com.thisismine.myapplication.core.ui.FormDialogShell
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.StatusChip
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.reminders.data.WagChangeReminderRepository
import com.thisismine.myapplication.feature.reminders.notifications.AlarmReminderScheduler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

data class ReminderDraft(
    val editingId: String? = null,
    val title: String = "",
    val triggerAtKm: String = "",
    val triggerAtIsoDate: String = "",
    val repeatingIntervalKm: String = ""
)

@Composable
fun ServiceRemindersScreen(wagChangeRepository: WagChangeRepository) {
    val context = LocalContext.current
    val notificationScheduler = remember(context) { AlarmReminderScheduler(context.applicationContext) }
    val vm: ServiceRemindersViewModel = viewModel(
        factory = ServiceRemindersViewModelFactory(
            repository = WagChangeReminderRepository(wagChangeRepository),
            notificationScheduler = notificationScheduler
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val activeMotorcycle by wagChangeRepository.activeMotorcycleFlow.collectAsStateWithLifecycle()
    val pendingDelete = uiState.reminders.firstOrNull { it.id == uiState.pendingDeleteReminderId }

    var draft by remember { mutableStateOf(ReminderDraft()) }
    var showForm by remember { mutableStateOf(false) }
    val density = LocalCardDensity.current

    fun startAdd() {
        if (uiState.showArchivedReminders) return
        draft = ReminderDraft(triggerAtKm = uiState.currentOdometerKm.toString())
        showForm = true
    }

    fun closeForm() {
        draft = ReminderDraft()
        showForm = false
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.refresh(message = "Notifications enabled.")
    }
    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    LaunchedEffect(activeMotorcycle?.id) {
        vm.refresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(density.listContentPadding),
        verticalArrangement = Arrangement.spacedBy(density.sectionSpacing)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Service Reminders", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = uiState.showArchivedReminders,
                            onCheckedChange = vm::toggleShowArchivedReminders
                        )
                        Text("Show archived", modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(onClick = ::startAdd, enabled = !uiState.showArchivedReminders) {
                        Text("Add reminder")
                    }
                }
            }
        }

        if (uiState.message != null || uiState.error != null) {
            item {
                AppCardShell(
                    tonal = uiState.error == null,
                    contentPadding = PaddingValues(density.cardPadding)
                ) {
                    Text(
                        text = uiState.error ?: uiState.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            Text(
                "Current odometer: ${uiState.currentOdometerKm} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                "Completed one-time reminders are archived so the history stays on record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.showArchivedReminders && uiState.archivedReminderCount > 0) {
            item {
                Text(
                    text = "Archived reminders: ${uiState.archivedReminderCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!notificationsGranted) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(density.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications are disabled. Enable them to receive reminder alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        ) {
                            Text("Enable notifications")
                        }
                    }
                }
            }
        }

        if (uiState.reminders.isEmpty()) {
            item {
                Text(
                    if (uiState.showArchivedReminders) {
                        "No reminders to show."
                    } else {
                        "No reminders yet. Create one to track upcoming maintenance."
                    }
                )
            }
        } else {
            items(uiState.reminders, key = { it.id }) { reminder ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(density.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardHeaderRow(icon = Icons.Filled.AddAlert, title = reminder.title)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(text = reminder.dueTone.label(), tone = reminder.dueTone)
                            if (reminder.isArchived) {
                                StatusChip(text = "Archived", tone = StatusTone.Neutral)
                            }
                            Text(
                                text = reminder.dueText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!reminder.isArchived) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactIconAction(
                                    icon = Icons.Filled.Snooze,
                                    contentDescription = "Snooze reminder",
                                    onClick = { vm.snoozeReminder(reminder.id) }
                                )
                                CompactIconAction(
                                    icon = Icons.Filled.CheckCircle,
                                    contentDescription = "Mark reminder done",
                                    onClick = { vm.markDoneReminder(reminder.id) }
                                )
                                CompactIconAction(
                                    icon = Icons.Filled.Edit,
                                    contentDescription = "Edit reminder",
                                    onClick = {
                                        draft = ReminderDraft(
                                            editingId = reminder.id,
                                            title = reminder.title,
                                            triggerAtKm = reminder.triggerAtKm.toString(),
                                            triggerAtIsoDate = reminder.triggerAtIsoDate.orEmpty(),
                                            repeatingIntervalKm = reminder.repeatingIntervalKm?.toString().orEmpty()
                                        )
                                        showForm = true
                                    }
                                )
                                CompactIconAction(
                                    icon = Icons.Filled.Delete,
                                    contentDescription = "Delete reminder",
                                    onClick = { vm.requestDeleteReminder(reminder.id) }
                                )
                            }
                        } else {
                            Text(
                                text = "Archived reminders are view-only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text("Base trigger: ${reminder.triggerAtKm} km")
                        if (reminder.repeatingIntervalKm != null) {
                            Text("Next trigger: ${reminder.nextTriggerAtKm} km")
                        }
                        if (!reminder.triggerAtIsoDate.isNullOrBlank()) {
                            Text("Trigger date: ${reminder.triggerAtIsoDate}")
                        }
                        if (reminder.repeatingIntervalKm != null) {
                            Text("Repeating reminder")
                            Text("Repeats every ${reminder.repeatingIntervalKm} km")
                            Text("Completed ${reminder.completionCount} times")
                        } else {
                            Text("One-time reminder")
                        }
                        if (reminder.lastTriggeredAtKm != null) {
                            Text("Last trigger: ${reminder.lastTriggeredAtKm} km")
                        }
                        if (!reminder.lastCompletedAtIsoDate.isNullOrBlank()) {
                            Text("Last completed: ${reminder.lastCompletedAtIsoDate}")
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        FormDialogShell(
            title = if (draft.editingId == null) "Add Reminder" else "Edit Reminder",
            confirmText = if (draft.editingId == null) "Add" else "Update",
            onConfirm = {
                val ok = vm.saveReminder(
                    editingId = draft.editingId,
                    title = draft.title,
                    triggerAtKmInput = draft.triggerAtKm,
                    triggerAtIsoDateInput = draft.triggerAtIsoDate,
                    repeatingIntervalKmInput = draft.repeatingIntervalKm
                )
                if (ok) closeForm()
            },
            onDismiss = ::closeForm,
            error = uiState.error,
            message = uiState.message
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                label = { Text("Reminder title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.triggerAtKm,
                onValueChange = { draft = draft.copy(triggerAtKm = it) },
                label = { Text("Trigger odometer km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.repeatingIntervalKm,
                onValueChange = { draft = draft.copy(repeatingIntervalKm = it) },
                label = { Text("Repeat interval km (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            DatePickerField(
                value = draft.triggerAtIsoDate,
                onValueChange = { draft = draft.copy(triggerAtIsoDate = it) },
                label = "Trigger date (optional)",
                modifier = Modifier.fillMaxWidth(),
                allowClear = true
            )
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDeleteReminder,
            title = { Text("Delete reminder") },
            text = { Text("Are you sure you want to delete \"${pendingDelete.title}\"?") },
            confirmButton = { TextButton(onClick = vm::confirmDeleteReminder) { Text("Delete") } },
            dismissButton = { TextButton(onClick = vm::cancelDeleteReminder) { Text("Cancel") } }
        )
    }
}

private fun StatusTone.label(): String = when (this) {
    StatusTone.Critical -> "Overdue"
    StatusTone.Warning -> "Due soon"
    StatusTone.Positive -> "On track"
    StatusTone.Neutral -> "Pending"
    StatusTone.Info -> "Info"
}

