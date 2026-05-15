package com.thisismine.myapplication.feature.reminders.notifications

import com.thisismine.myapplication.domain.model.ServiceReminder
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionEngineTest {

    @Test
    fun snooze_setsTriggerDateToTomorrow() {
        val reminder = sampleReminder(triggerAtIsoDate = null, repeatingIntervalKm = null)
        val today = LocalDate.of(2026, 4, 7)

        val result = ReminderActionEngine.apply(
            action = ReminderActionType.Snooze,
            reminder = reminder,
            currentOdometerKm = 15200,
            today = today
        )

        assertFalse(result.shouldArchive)
        assertEquals("2026-04-08", result.updatedReminder?.triggerAtIsoDate)
    }

    @Test
    fun snooze_keepsLaterExistingDate() {
        val reminder = sampleReminder(triggerAtIsoDate = "2026-04-20", repeatingIntervalKm = null)
        val today = LocalDate.of(2026, 4, 7)

        val result = ReminderActionEngine.apply(
            action = ReminderActionType.Snooze,
            reminder = reminder,
            currentOdometerKm = 15200,
            today = today
        )

        assertEquals("2026-04-20", result.updatedReminder?.triggerAtIsoDate)
    }

    @Test
    fun snooze_invalidDate_fallsBackToTomorrow() {
        val reminder = sampleReminder(triggerAtIsoDate = "not-a-date", repeatingIntervalKm = null)
        val today = LocalDate.of(2026, 4, 7)

        val result = ReminderActionEngine.apply(
            action = ReminderActionType.Snooze,
            reminder = reminder,
            currentOdometerKm = 15200,
            today = today
        )

        assertEquals("2026-04-08", result.updatedReminder?.triggerAtIsoDate)
    }

    @Test
    fun markDone_repeating_setsLastTriggeredAtCurrentOdo_andKeepsReminder() {
        val reminder = sampleReminder(triggerAtIsoDate = "2026-04-07", repeatingIntervalKm = 1000)

        val result = ReminderActionEngine.apply(
            action = ReminderActionType.MarkDone,
            reminder = reminder,
            currentOdometerKm = 18050,
            today = LocalDate.of(2026, 4, 7)
        )

        assertFalse(result.shouldArchive)
        assertEquals(18050, result.updatedReminder?.lastTriggeredAtKm)
        assertNull(result.updatedReminder?.triggerAtIsoDate)
        assertEquals(1, result.updatedReminder?.completionCount)
        assertEquals("2026-04-07", result.updatedReminder?.lastCompletedAtIsoDate)
    }

    @Test
    fun markDone_oneTime_deletesReminder() {
        val reminder = sampleReminder(triggerAtIsoDate = "2026-04-07", repeatingIntervalKm = null)

        val result = ReminderActionEngine.apply(
            action = ReminderActionType.MarkDone,
            reminder = reminder,
            currentOdometerKm = 18050,
            today = LocalDate.of(2026, 4, 7)
        )

        assertTrue(result.shouldArchive)
        assertNull(result.updatedReminder)
    }

    private fun sampleReminder(
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?
    ): ServiceReminder {
        return ServiceReminder(
            id = "r1",
            motorcycleId = "bike-1",
            title = "Oil Change",
            triggerAtKm = 15000,
            triggerAtIsoDate = triggerAtIsoDate,
            repeatingIntervalKm = repeatingIntervalKm,
            lastTriggeredAtKm = null,
            completionCount = 0,
            lastCompletedAtIsoDate = null,
            isArchived = false
        )
    }
}
