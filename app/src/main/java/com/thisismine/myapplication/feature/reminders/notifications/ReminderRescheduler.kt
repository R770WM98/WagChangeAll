package com.thisismine.myapplication.feature.reminders.notifications

import com.thisismine.myapplication.feature.reminders.data.ReminderRepository

@Suppress("unused")
class ReminderRescheduler(
    private val repository: ReminderRepository,
    private val scheduler: ReminderNotificationScheduler
) {
    fun rescheduleAll() {
        val reminders = repository.getReminders()
        val currentOdometerKm = repository.getCurrentOdometerKm()
        scheduler.syncReminders(reminders, currentOdometerKm)
    }
}
