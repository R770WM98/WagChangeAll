package com.thisismine.myapplication.feature.reminders.notifications

import com.thisismine.myapplication.domain.model.ServiceReminder

interface ReminderNotificationScheduler {
    fun syncReminders(reminders: List<ServiceReminder>, currentOdometerKm: Int)
    fun cancelReminder(reminderId: String)
}

