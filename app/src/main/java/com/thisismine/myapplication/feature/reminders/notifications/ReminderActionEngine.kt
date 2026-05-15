package com.thisismine.myapplication.feature.reminders.notifications

import com.thisismine.myapplication.domain.model.ServiceReminder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ReminderActionType {
    Snooze,
    MarkDone
}

data class ReminderActionResult(
    val updatedReminder: ServiceReminder? = null,
    val shouldArchive: Boolean = false,
    val message: String
)

object ReminderActionEngine {
    fun apply(
        action: ReminderActionType,
        reminder: ServiceReminder,
        currentOdometerKm: Int,
        today: LocalDate = LocalDate.now()
    ): ReminderActionResult {
        return when (action) {
            ReminderActionType.Snooze -> {
                val tomorrow = today.plusDays(1)
                val existing = reminder.triggerAtIsoDate.toLocalDateOrNull()
                val effectiveDate = if (existing != null && existing.isAfter(tomorrow)) existing else tomorrow
                val snoozedDate = effectiveDate.toString()
                ReminderActionResult(
                    updatedReminder = reminder.copy(triggerAtIsoDate = snoozedDate),
                    message = "Reminder snoozed until $snoozedDate"
                )
            }
            ReminderActionType.MarkDone -> {
                val interval = reminder.repeatingIntervalKm
                if (interval == null || interval <= 0) {
                    ReminderActionResult(
                        shouldArchive = true,
                        message = "Reminder completed and archived"
                    )
                } else {
                    ReminderActionResult(
                        updatedReminder = reminder.copy(
                            lastTriggeredAtKm = currentOdometerKm,
                            triggerAtIsoDate = null,
                            completionCount = reminder.completionCount + 1,
                            lastCompletedAtIsoDate = today.toString(),
                            isArchived = false
                        ),
                        message = "Reminder completed. Next due after $interval km"
                    )
                }
            }
        }
    }
}

private fun String?.toLocalDateOrNull(): LocalDate? {
    if (this.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}
