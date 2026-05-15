package com.thisismine.myapplication.feature.reminders.data

import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.ServiceReminder

class WagChangeReminderRepository(
    private val wagChangeRepository: WagChangeRepository
) : ReminderRepository {
    override fun getCurrentOdometerKm(): Int = wagChangeRepository.getProfile().currentOdometerKm

    override fun getReminders(): List<ServiceReminder> = wagChangeRepository.getReminders()

    override fun getArchivedReminders(): List<ServiceReminder> = wagChangeRepository.getArchivedReminders()

    override fun addReminder(
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int,
        lastCompletedAtIsoDate: String?,
        isArchived: Boolean
    ) {
        wagChangeRepository.addReminder(
            title = title,
            triggerAtKm = triggerAtKm,
            triggerAtIsoDate = triggerAtIsoDate,
            repeatingIntervalKm = repeatingIntervalKm,
            lastTriggeredAtKm = lastTriggeredAtKm,
            completionCount = completionCount,
            lastCompletedAtIsoDate = lastCompletedAtIsoDate,
            isArchived = isArchived
        )
    }

    override fun updateReminder(
        id: String,
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int,
        lastCompletedAtIsoDate: String?,
        isArchived: Boolean
    ) {
        wagChangeRepository.updateReminder(
            id = id,
            title = title,
            triggerAtKm = triggerAtKm,
            triggerAtIsoDate = triggerAtIsoDate,
            repeatingIntervalKm = repeatingIntervalKm,
            lastTriggeredAtKm = lastTriggeredAtKm,
            completionCount = completionCount,
            lastCompletedAtIsoDate = lastCompletedAtIsoDate,
            isArchived = isArchived
        )
    }

    override fun archiveReminder(id: String, lastCompletedAtIsoDate: String?) {
        wagChangeRepository.archiveReminder(id, lastCompletedAtIsoDate)
    }

    override fun logReminderCompletionAsService(
        reminderId: String,
        title: String,
        dateIso: String,
        odometerKm: Int
    ) {
        wagChangeRepository.addMaintenanceEntry(
            title = title,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = 0.0,
            notes = "Auto-logged from reminder completion ($reminderId)",
            costLevelOverride = "AUTO",
            imageUris = emptyList()
        )
    }

    override fun deleteReminder(id: String) {
        wagChangeRepository.deleteReminder(id)
    }
}
