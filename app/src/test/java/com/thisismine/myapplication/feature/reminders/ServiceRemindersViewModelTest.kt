package com.thisismine.myapplication.feature.reminders

import com.thisismine.myapplication.domain.model.ServiceReminder
import com.thisismine.myapplication.feature.reminders.data.ReminderRepository
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRemindersViewModelTest {

    @Test
    fun saveReminder_syncsNotifications() {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = ServiceRemindersViewModel(repository, scheduler)

        val ok = viewModel.saveReminder(
            editingId = null,
            title = "Chain Service",
            triggerAtKmInput = "12500",
            triggerAtIsoDateInput = "2026-04-09",
            repeatingIntervalKmInput = "1000"
        )

        assertTrue(ok)
        assertEquals(2, repository.reminderStore.size)
        assertTrue(scheduler.syncCalls >= 2) // init + save refresh
        assertEquals(repository.getCurrentOdometerKm(), scheduler.lastCurrentOdometer)
    }

    @Test
    fun confirmDeleteReminder_cancelsReminderNotification() {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = ServiceRemindersViewModel(repository, scheduler)
        val reminderId = repository.reminderStore.first().id

        viewModel.requestDeleteReminder(reminderId)
        viewModel.confirmDeleteReminder()

        assertFalse(repository.reminderStore.any { it.id == reminderId })
        assertEquals(reminderId, scheduler.lastCancelledReminderId)
    }

    @Test
    fun snoozeReminder_pushesTriggerDateForward() {
        val repository = FakeReminderRepository()
        repository.reminderStore[0] = repository.reminderStore[0].copy(triggerAtIsoDate = null)
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = ServiceRemindersViewModel(repository, scheduler)
        val reminderId = repository.reminderStore.first().id

        val ok = viewModel.snoozeReminder(reminderId)

        assertTrue(ok)
        assertEquals(LocalDate.now().plusDays(1).toString(), repository.reminderStore.first().triggerAtIsoDate)
    }

    @Test
    fun snoozeReminder_keepsLaterDateWhenAlreadyFuture() {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = ServiceRemindersViewModel(repository, scheduler)
        val reminderId = repository.reminderStore.first().id

        val ok = viewModel.snoozeReminder(reminderId)

        assertTrue(ok)
        assertEquals(LocalDate.now().plusDays(2).toString(), repository.reminderStore.first().triggerAtIsoDate)
    }

    @Test
    fun markDoneReminder_advancesRepeatingReminder_byRecordingCurrentOdometer() {
        val repository = FakeReminderRepository()
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = ServiceRemindersViewModel(repository, scheduler)
        val reminderId = repository.reminderStore.first().id

        val ok = viewModel.markDoneReminder(reminderId)

        assertTrue(ok)
        assertEquals(repository.getCurrentOdometerKm(), repository.reminderStore.first().lastTriggeredAtKm)
        assertFalse(repository.reminderStore.isEmpty())
        assertEquals(1, repository.reminderStore.first().completionCount)
        assertEquals(LocalDate.now().toString(), repository.reminderStore.first().lastCompletedAtIsoDate)
    }
}

private class FakeReminderRepository : ReminderRepository {
    val reminderStore = mutableListOf(
        ServiceReminder(
            id = "r1",
            motorcycleId = "bike-1",
            title = "Oil Change",
            triggerAtKm = 12000,
            triggerAtIsoDate = LocalDate.now().plusDays(2).toString(),
            repeatingIntervalKm = 1000,
            lastTriggeredAtKm = null,
            completionCount = 0,
            lastCompletedAtIsoDate = null,
            isArchived = false
        )
    )

    override fun getCurrentOdometerKm(): Int = 11800

     override fun getReminders(): List<ServiceReminder> = reminderStore.filter { !it.isArchived }

    override fun getArchivedReminders(): List<ServiceReminder> = reminderStore.filter { it.isArchived }

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
        reminderStore.add(
            ServiceReminder(
                id = "r${reminderStore.size + 1}",
                motorcycleId = "bike-1",
                title = title,
                triggerAtKm = triggerAtKm,
                triggerAtIsoDate = triggerAtIsoDate,
                repeatingIntervalKm = repeatingIntervalKm,
                lastTriggeredAtKm = lastTriggeredAtKm,
                completionCount = completionCount,
                lastCompletedAtIsoDate = lastCompletedAtIsoDate,
                isArchived = isArchived
            )
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
        val index = reminderStore.indexOfFirst { it.id == id }
        if (index < 0) return
        reminderStore[index] = reminderStore[index].copy(
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
        val index = reminderStore.indexOfFirst { it.id == id }
        if (index < 0) return
        reminderStore[index] = reminderStore[index].copy(
            isArchived = true,
            lastCompletedAtIsoDate = lastCompletedAtIsoDate,
            completionCount = reminderStore[index].completionCount + 1
        )
    }

    override fun logReminderCompletionAsService(
        reminderId: String,
        title: String,
        dateIso: String,
        odometerKm: Int
    ) = Unit

    override fun deleteReminder(id: String) {
        reminderStore.removeAll { it.id == id }
    }
}

private class FakeReminderNotificationScheduler : ReminderNotificationScheduler {
    var syncCalls: Int = 0
    var lastCurrentOdometer: Int = 0
    var lastCancelledReminderId: String? = null

    override fun syncReminders(reminders: List<ServiceReminder>, currentOdometerKm: Int) {
        syncCalls += 1
        lastCurrentOdometer = currentOdometerKm
    }

    override fun cancelReminder(reminderId: String) {
        lastCancelledReminderId = reminderId
    }
}
