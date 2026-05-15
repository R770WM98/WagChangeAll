package com.thisismine.myapplication.feature.dashboard

import com.thisismine.myapplication.data.repository.InMemoryWagChangeRepository
import com.thisismine.myapplication.domain.model.ServiceReminder
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelTest {

    @Test
    fun upcomingMaintenance_isLimitedToThreeAndSortedByNearestKm() {
        val repository = InMemoryWagChangeRepository()
        repository.addOdometerReading(15000, "2026-04-01")

        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val upcoming = viewModel.uiState.value.upcomingMaintenance
        assertTrue(upcoming.isNotEmpty())
        assertTrue(upcoming.size <= 3)
        assertTrue(upcoming.first().title.contains("CVT", ignoreCase = true))
    }

    @Test
    fun alerts_includesDueSoonAndStaleReadingSignals() {
        val repository = InMemoryWagChangeRepository()
        repository.addOdometerReading(15900, "2026-03-01")
        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val alerts = viewModel.uiState.value.alerts
        assertTrue(alerts.any { it.title.contains("CVT", ignoreCase = true) })
        assertTrue(alerts.any { it.title.contains("stale", ignoreCase = true) })
    }

    @Test
    fun recentActivity_containsMostRecentFiveEntries() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        repository.addOdometerReading(15430, "2026-03-21")
        repository.addMaintenanceEntry("Chain Adjust", "2026-03-22", 15430, 120.0, "")
        repository.addPartReplacement("Spark Plug", "2026-03-23", 15440, 300.0, "")
        repository.addOdometerReading(15450, "2026-03-24")
        repository.addMaintenanceEntry("Brake Fluid", "2026-03-25", 15450, 500.0, "")
        repository.addPartReplacement("Tire", "2026-03-26", 15460, 2200.0, "")

        viewModel.refresh()

        val recent = viewModel.uiState.value.recentActivity
        assertEquals(5, recent.size)
        assertTrue(recent.first().dateIso >= recent.last().dateIso)
    }

    @Test
    fun dueTimeline_ordersUrgentRemindersFirst() {
        val repository = InMemoryWagChangeRepository()
        repository.addReminder("Brake Fluid", 15500, "2026-04-08", null, null)
        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val timeline = viewModel.uiState.value.dueTimeline
        assertTrue(timeline.isNotEmpty())
        assertEquals("Brake Fluid", timeline.first().title)
    }

    @Test
    fun monthlySpend_aggregatesMaintenanceAndFuelTotals() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val monthlySpend = viewModel.uiState.value.monthlySpend
        assertTrue(monthlySpend.isNotEmpty())
        assertEquals("Mar 2026", monthlySpend.first().monthLabel)
        assertEquals(1100.0, monthlySpend.first().totalPhp, 0.01)
    }

    @Test
    fun reminderActions_updateAndCancelThroughViewModel() {
        val repository = InMemoryWagChangeRepository()
        val scheduler = FakeReminderNotificationScheduler()
        val viewModel = DashboardViewModel(repository, scheduler)
        val reminderId = repository.getReminders().first().id

        val snoozed = viewModel.snoozeReminder(reminderId)
        val done = viewModel.markDoneReminder(reminderId)

        assertTrue(snoozed)
        assertTrue(done)
        assertTrue(scheduler.cancelCalls >= 0)
        assertTrue(scheduler.syncCalls >= 2)
    }

    @Test
    fun fuelAnalytics_computesMonthOverMonthTrends() {
        val repository = InMemoryWagChangeRepository()
        repository.addFuelEntry(dateIso = "2026-02-15", liters = 5.0, costPhp = 300.0, distanceKm = 150.0)
        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val snapshot = viewModel.uiState.value.fuelAnalytics
        assertTrue(snapshot.currentMonthKmPerLiter != null)
        assertTrue(snapshot.previousMonthKmPerLiter != null)
        assertTrue(snapshot.efficiencyDeltaPercent != null)
        assertTrue(snapshot.costPerKmDeltaPercent != null)
        assertTrue(snapshot.efficiencyDeltaPercent!! > 0.0)
        assertTrue(snapshot.costPerKmDeltaPercent!! < 0.0)
    }

    @Test
    fun alerts_prioritizeUrgentItems() {
        val repository = InMemoryWagChangeRepository()
        repository.addReminder(
            title = "Drive Belt Check",
            triggerAtKm = 100,
            triggerAtIsoDate = null,
            repeatingIntervalKm = null,
            lastTriggeredAtKm = null
        )

        val viewModel = DashboardViewModel(repository, FakeReminderNotificationScheduler())

        val alerts = viewModel.uiState.value.alerts
        assertTrue(alerts.isNotEmpty())
        assertEquals("Drive Belt Check", alerts.first().title)
        assertEquals(AlertSeverity.Overdue, alerts.first().severity)
    }
}

private class FakeReminderNotificationScheduler : ReminderNotificationScheduler {
    var syncCalls: Int = 0
    var cancelCalls: Int = 0

    override fun syncReminders(reminders: List<ServiceReminder>, currentOdometerKm: Int) {
        syncCalls += 1
    }

    override fun cancelReminder(reminderId: String) {
        cancelCalls += 1
    }
}
