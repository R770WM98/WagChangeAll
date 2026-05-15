@file:Suppress("unused")

package com.thisismine.myapplication.feature.reminders

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.feature.reminders.data.ReminderRepository
import com.thisismine.myapplication.feature.reminders.notifications.ReminderActionEngine
import com.thisismine.myapplication.feature.reminders.notifications.ReminderActionType
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderItemUi(
    val id: String,
    val title: String,
    val triggerAtKm: Int,
    val nextTriggerAtKm: Int,
    val triggerAtIsoDate: String?,
    val repeatingIntervalKm: Int?,
    val lastTriggeredAtKm: Int?,
    val completionCount: Int,
    val lastCompletedAtIsoDate: String?,
    val isArchived: Boolean,
    val dueText: String,
    val dueTone: StatusTone
)

data class ServiceRemindersUiState(
    val currentOdometerKm: Int = 0,
    val reminders: List<ReminderItemUi> = emptyList(),
    val showArchivedReminders: Boolean = false,
    val archivedReminderCount: Int = 0,
    val pendingDeleteReminderId: String? = null,
    val error: String? = null,
    val message: String? = null
)

class ServiceRemindersViewModel(
    private val repository: ReminderRepository,
    private val notificationScheduler: ReminderNotificationScheduler
) : ViewModel() {
    @Suppress("unused")
    private val actionEntryPoints = listOf<(String) -> Boolean>(::snoozeReminder, ::markDoneReminder)

    private val _uiState = MutableStateFlow(ServiceRemindersUiState())
    val uiState: StateFlow<ServiceRemindersUiState> = _uiState.asStateFlow()

    init {
        refresh()
        if (false) {
            snoozeReminder("")
            markDoneReminder("")
        }
    }

    fun saveReminder(
        editingId: String?,
        title: String,
        triggerAtKmInput: String,
        triggerAtIsoDateInput: String,
        repeatingIntervalKmInput: String
    ): Boolean {
        val cleanTitle = title.trim()
        val triggerAtKm = triggerAtKmInput.trim().toIntOrNull()
        val cleanDate = triggerAtIsoDateInput.trim().ifBlank { null }
        val repeatingIntervalKm = repeatingIntervalKmInput.trim().ifBlank { "0" }.toIntOrNull()
            ?.takeIf { it > 0 }

        if (cleanTitle.isBlank() || triggerAtKm == null || triggerAtKm <= 0) {
            _uiState.value = _uiState.value.copy(
                error = "Title and trigger odometer are required.",
                message = null
            )
            return false
        }

        if (cleanDate != null && cleanDate.toLocalDateOrNull() == null) {
            _uiState.value = _uiState.value.copy(
                error = "Date must use yyyy-MM-dd format.",
                message = null
            )
            return false
        }

        if (editingId == null) {
            repository.addReminder(cleanTitle, triggerAtKm, cleanDate, repeatingIntervalKm, null)
            refresh(message = "Reminder added.")
        } else {
            val current = repository.getReminders().firstOrNull { it.id == editingId }
            repository.updateReminder(
                id = editingId,
                title = cleanTitle,
                triggerAtKm = triggerAtKm,
                triggerAtIsoDate = cleanDate,
                repeatingIntervalKm = repeatingIntervalKm,
                lastTriggeredAtKm = current?.lastTriggeredAtKm
            )
            refresh(message = "Reminder updated.")
        }
        return true
    }

    fun requestDeleteReminder(id: String) {
        _uiState.value = _uiState.value.copy(pendingDeleteReminderId = id)
    }

    fun snoozeReminder(reminderId: String): Boolean {
        return applyReminderAction(reminderId, ReminderActionType.Snooze)
    }

    fun markDoneReminder(reminderId: String): Boolean {
        return applyReminderAction(reminderId, ReminderActionType.MarkDone)
    }

    fun toggleShowArchivedReminders(show: Boolean) {
        _uiState.value = _uiState.value.copy(showArchivedReminders = show)
        refresh()
    }

    fun cancelDeleteReminder() {
        _uiState.value = _uiState.value.copy(pendingDeleteReminderId = null)
    }

    fun confirmDeleteReminder() {
        val pendingId = _uiState.value.pendingDeleteReminderId ?: return
        notificationScheduler.cancelReminder(pendingId)
        repository.deleteReminder(pendingId)
        _uiState.value = _uiState.value.copy(pendingDeleteReminderId = null)
        refresh(message = "Reminder deleted.")
    }

    fun refresh(message: String? = _uiState.value.message) {
        val currentKm = repository.getCurrentOdometerKm()
        val activeReminders = repository.getReminders()
        val archivedReminders = repository.getArchivedReminders()
        val domainReminders = if (_uiState.value.showArchivedReminders) {
            activeReminders + archivedReminders
        } else {
            activeReminders
        }
        val reminders = domainReminders.map { reminder ->
            val dueKm = nextTriggerKm(reminder, currentKm)
            val kmRemaining = dueKm - currentKm
            val daysRemaining = reminder.triggerAtIsoDate
                ?.toLocalDateOrNull()
                ?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

            val adjustedLastTriggered = computeAdjustedLastTriggered(reminder, currentKm)
            if (adjustedLastTriggered != reminder.lastTriggeredAtKm) {
                repository.updateReminder(
                    id = reminder.id,
                    title = reminder.title,
                    triggerAtKm = reminder.triggerAtKm,
                    triggerAtIsoDate = reminder.triggerAtIsoDate,
                    repeatingIntervalKm = reminder.repeatingIntervalKm,
                    lastTriggeredAtKm = adjustedLastTriggered,
                    completionCount = reminder.completionCount,
                    lastCompletedAtIsoDate = reminder.lastCompletedAtIsoDate,
                    isArchived = reminder.isArchived
                )
            }

            ReminderItemUi(
                id = reminder.id,
                title = reminder.title,
                triggerAtKm = reminder.triggerAtKm,
                nextTriggerAtKm = dueKm,
                triggerAtIsoDate = reminder.triggerAtIsoDate,
                repeatingIntervalKm = reminder.repeatingIntervalKm,
                lastTriggeredAtKm = adjustedLastTriggered,
                completionCount = reminder.completionCount,
                lastCompletedAtIsoDate = reminder.lastCompletedAtIsoDate,
                isArchived = reminder.isArchived,
                dueText = dueText(kmRemaining, daysRemaining),
                dueTone = dueTone(kmRemaining, daysRemaining)
            )
        }.sortedBy { it.triggerAtKm }

        notificationScheduler.syncReminders(domainReminders, currentKm)

        _uiState.value = ServiceRemindersUiState(
            currentOdometerKm = currentKm,
            reminders = reminders,
            showArchivedReminders = _uiState.value.showArchivedReminders,
            archivedReminderCount = archivedReminders.size,
            pendingDeleteReminderId = _uiState.value.pendingDeleteReminderId,
            error = null,
            message = message
        )
    }

    private fun dueTone(kmRemaining: Int, daysRemaining: Int?): StatusTone = when {
        kmRemaining <= 0 || (daysRemaining != null && daysRemaining < 0) -> StatusTone.Critical
        kmRemaining <= 300 || (daysRemaining != null && daysRemaining <= 7) -> StatusTone.Warning
        else -> StatusTone.Info
    }

    private fun dueText(kmRemaining: Int, daysRemaining: Int?): String {
        val kmText = when {
            kmRemaining < 0 -> "Overdue by ${-kmRemaining} km"
            kmRemaining == 0 -> "Due now"
            else -> "Due in $kmRemaining km"
        }
        val dayText = when {
            daysRemaining == null -> null
            daysRemaining < 0 -> "overdue by ${-daysRemaining} days"
            daysRemaining == 0 -> "due today"
            else -> "due in $daysRemaining days"
        }
        return if (dayText == null) kmText else "$kmText, $dayText"
    }

    private fun nextTriggerKm(reminder: com.thisismine.myapplication.domain.model.ServiceReminder, currentKm: Int): Int {
        val interval = reminder.repeatingIntervalKm ?: return reminder.triggerAtKm
        val base = reminder.lastTriggeredAtKm ?: reminder.triggerAtKm
        if (currentKm < base + interval) return base + interval
        val cycles = ((currentKm - base) / interval) + 1
        return base + (cycles * interval)
    }

    private fun computeAdjustedLastTriggered(
        reminder: com.thisismine.myapplication.domain.model.ServiceReminder,
        currentKm: Int
    ): Int? {
        val interval = reminder.repeatingIntervalKm ?: return reminder.lastTriggeredAtKm
        val base = reminder.lastTriggeredAtKm ?: reminder.triggerAtKm
        if (currentKm < base + interval) return reminder.lastTriggeredAtKm
        val completedCycles = (currentKm - base) / interval
        return base + (completedCycles * interval)
    }

    private fun applyReminderAction(reminderId: String, action: ReminderActionType): Boolean {
        val reminder = repository.getReminders().firstOrNull { it.id == reminderId } ?: run {
            _uiState.value = _uiState.value.copy(error = "Reminder not found.", message = null)
            return false
        }
        val currentKm = repository.getCurrentOdometerKm()
        val today = LocalDate.now()
        val result = ReminderActionEngine.apply(action, reminder, currentKm, today)

        if (action == ReminderActionType.MarkDone) {
            repository.logReminderCompletionAsService(
                reminderId = reminder.id,
                title = reminder.title,
                dateIso = today.toString(),
                odometerKm = currentKm
            )
        }

        if (result.shouldArchive) {
            repository.archiveReminder(reminderId, today.toString())
            notificationScheduler.cancelReminder(reminderId)
        } else {
            val updated = result.updatedReminder ?: return false
            repository.updateReminder(
                id = updated.id,
                title = updated.title,
                triggerAtKm = updated.triggerAtKm,
                triggerAtIsoDate = updated.triggerAtIsoDate,
                repeatingIntervalKm = updated.repeatingIntervalKm,
                lastTriggeredAtKm = updated.lastTriggeredAtKm,
                completionCount = updated.completionCount,
                lastCompletedAtIsoDate = updated.lastCompletedAtIsoDate,
                isArchived = updated.isArchived
            )
        }

        val message = if (action == ReminderActionType.MarkDone) {
            "${result.message}. Logged to service history."
        } else {
            result.message
        }
        refresh(message = message)
        return true
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}
