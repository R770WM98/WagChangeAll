package com.thisismine.myapplication.feature.reminders.notifications

import com.thisismine.myapplication.domain.model.ServiceReminder
import com.thisismine.myapplication.feature.reminders.data.ReminderRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderReschedulerTest {

    @Test
    fun rescheduleAll_syncsRepositoryRemindersWithCurrentOdometer() {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderNotificationScheduler()

        ReminderRescheduler(repository, scheduler).rescheduleAll()

        assertEquals(1, scheduler.syncCalls)
        assertEquals(12450, scheduler.lastOdometerKm)
        assertEquals(repository.reminderStore, scheduler.lastReminders)
    }
}

private class FakeReminderRepository : ReminderRepository {
    val reminderStore = listOf(
        ServiceReminder(
            id = "r1",
            motorcycleId = "bike-1",
            title = "Oil Change",
            triggerAtKm = 13000,
            triggerAtIsoDate = "2026-05-10",
            repeatingIntervalKm = 1000,
            lastTriggeredAtKm = 12000,
            completionCount = 0,
            lastCompletedAtIsoDate = null,
            isArchived = false
        )
    )

    override fun getCurrentOdometerKm(): Int = 12450

    override fun getReminders(): List<ServiceReminder> = reminderStore

    override fun getArchivedReminders(): List<ServiceReminder> = emptyList()

    override fun addReminder(
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int,
        lastCompletedAtIsoDate: String?,
        isArchived: Boolean
    ) = Unit

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
    ) = Unit

    override fun archiveReminder(id: String, lastCompletedAtIsoDate: String?) = Unit

    override fun logReminderCompletionAsService(
        reminderId: String,
        title: String,
        dateIso: String,
        odometerKm: Int
    ) = Unit

    override fun deleteReminder(id: String) = Unit
}

private class FakeReminderNotificationScheduler : ReminderNotificationScheduler {
    var syncCalls: Int = 0
    var lastOdometerKm: Int = 0
    var lastReminders: List<ServiceReminder> = emptyList()

    override fun syncReminders(reminders: List<ServiceReminder>, currentOdometerKm: Int) {
        syncCalls += 1
        lastOdometerKm = currentOdometerKm
        lastReminders = reminders
    }

    override fun cancelReminder(reminderId: String) = Unit
}
