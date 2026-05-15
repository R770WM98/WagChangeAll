package com.thisismine.myapplication.feature.reminders.data

import com.thisismine.myapplication.domain.model.ServiceReminder

interface ReminderRepository {
    fun getCurrentOdometerKm(): Int
    fun getReminders(): List<ServiceReminder>
    fun getArchivedReminders(): List<ServiceReminder>
    fun addReminder(
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int? = null,
        completionCount: Int = 0,
        lastCompletedAtIsoDate: String? = null,
        isArchived: Boolean = false
    )
    fun updateReminder(
        id: String,
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int? = null,
        completionCount: Int = 0,
        lastCompletedAtIsoDate: String? = null,
        isArchived: Boolean = false
    )
    fun archiveReminder(id: String, lastCompletedAtIsoDate: String? = null)
    fun logReminderCompletionAsService(
        reminderId: String,
        title: String,
        dateIso: String,
        odometerKm: Int
    )
    fun deleteReminder(id: String)
}
