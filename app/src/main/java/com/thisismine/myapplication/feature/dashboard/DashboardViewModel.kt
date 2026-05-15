@file:Suppress("unused")

package com.thisismine.myapplication.feature.dashboard

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.ServiceReminder
import com.thisismine.myapplication.feature.reminders.notifications.ReminderActionEngine
import com.thisismine.myapplication.feature.reminders.notifications.ReminderActionType
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlertSeverity {
    Overdue,
    DueSoon,
    Info
}

data class DashboardAlert(
    val reminderId: String?,
    val title: String,
    val detail: String,
    val severity: AlertSeverity
)

data class OdometerSnapshot(
    val currentKm: Int = 0,
    val lastReadingDateIso: String? = null,
    val distanceLast30DaysKm: Int = 0,
    val readingCount: Int = 0
)

data class UpcomingMaintenanceItem(
    val title: String,
    val dueText: String
)

data class DueTimelineItem(
    val reminderId: String,
    val title: String,
    val detail: String,
    val severity: AlertSeverity
)

data class MonthlySpendItem(
    val monthLabel: String,
    val maintenanceTotalPhp: Double,
    val fuelTotalPhp: Double,
    val totalPhp: Double
)

data class RecentActivityItem(
    val dateIso: String,
    val title: String,
    val detail: String
)

    data class FuelAnalyticsSnapshot(
    val entryCount: Int = 0,
    val averageKmPerLiter: Double? = null,
    val costPerKm: Double? = null,
    val lastEntryDateIso: String? = null,
    val currentMonthKmPerLiter: Double? = null,
    val previousMonthKmPerLiter: Double? = null,
    val efficiencyDeltaPercent: Double? = null,
    val currentMonthCostPerKm: Double? = null,
    val previousMonthCostPerKm: Double? = null,
    val costPerKmDeltaPercent: Double? = null
)

data class DashboardUiState(
    val activeMotorcycle: MotorcycleProfile? = null,
    val motorcycleCount: Int = 0,
    val alerts: List<DashboardAlert> = emptyList(),
    val dueTimeline: List<DueTimelineItem> = emptyList(),
    val monthlySpend: List<MonthlySpendItem> = emptyList(),
    val odometer: OdometerSnapshot = OdometerSnapshot(),
    val upcomingMaintenance: List<UpcomingMaintenanceItem> = emptyList(),
    val recentActivity: List<RecentActivityItem> = emptyList(),
    val fuelAnalytics: FuelAnalyticsSnapshot = FuelAnalyticsSnapshot(),
    val error: String? = null,
    val message: String? = null
)

class DashboardViewModel(
    private val repository: WagChangeRepository,
    private val notificationScheduler: ReminderNotificationScheduler
) : ViewModel() {
    @Suppress("unused")
    private val selectMotorcycleEntryPoint = ::selectMotorcycle

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
        if (false) {
            selectMotorcycle("")
        }
    }

    fun refresh(message: String? = _uiState.value.message) {
        val motorcycles = repository.getMotorcycles()
        val active = repository.activeMotorcycleFlow.value

        if (active == null) {
            _uiState.value = DashboardUiState(motorcycleCount = motorcycles.size, error = null, message = message)
            return
        }

        val reminders = repository.getReminders()
        val readings = repository.getOdometerReadings()
        val maintenance = repository.getMaintenanceEntries()
        val parts = repository.getPartReplacements()
        val fuel = repository.getFuelEntries()

        _uiState.value = DashboardUiState(
            activeMotorcycle = active,
            motorcycleCount = motorcycles.size,
            alerts = buildAlerts(active.currentOdometerKm, reminders, readings),
            dueTimeline = buildDueTimeline(active.currentOdometerKm, reminders),
            monthlySpend = buildMonthlySpend(maintenance, fuel),
            odometer = buildOdometerSnapshot(active.currentOdometerKm, readings),
            upcomingMaintenance = buildUpcomingMaintenance(active.currentOdometerKm, reminders),
            fuelAnalytics = buildFuelAnalyticsSnapshot(active.id, fuel),
            recentActivity = buildRecentActivity(
                readings = readings,
                maintenanceTitles = maintenance.map {
                    RecentActivityItem(
                        dateIso = it.dateIso,
                        title = "Maintenance: ${it.title}",
                        detail = "${it.odometerKm} km - PHP ${"%.0f".format(it.totalCostPhp)}"
                    )
                },
                partTitles = parts.map {
                    RecentActivityItem(
                        dateIso = it.dateIso,
                        title = "Part: ${it.partName}",
                        detail = "${it.odometerKm} km - PHP ${"%.0f".format(it.totalCostPhp)}"
                    )
                }
            ),
            error = null,
            message = message
        )
    }

    fun selectMotorcycle(motorcycleId: String) {
        repository.setActiveMotorcycle(motorcycleId)
        refresh(message = "Active motorcycle changed.")
    }

    fun snoozeReminder(reminderId: String): Boolean = applyReminderAction(reminderId, ReminderActionType.Snooze)

    fun markDoneReminder(reminderId: String): Boolean = applyReminderAction(reminderId, ReminderActionType.MarkDone)

    private fun buildAlerts(
        currentOdometerKm: Int,
        reminders: List<ServiceReminder>,
        readings: List<com.thisismine.myapplication.domain.model.OdometerReading>
    ): List<DashboardAlert> {
        val today = LocalDate.now()
        val alerts = mutableListOf<DashboardAlert>()

        reminders.forEach { reminder ->
            val dueKm = reminder.effectiveDueKm(currentOdometerKm)
            val kmRemaining = dueKm - currentOdometerKm
            val dueDate = reminder.triggerAtIsoDate?.toLocalDateOrNull()
            val daysRemaining = dueDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() }

            when {
                kmRemaining <= 0 || (daysRemaining != null && daysRemaining < 0) -> {
                    alerts += DashboardAlert(
                        reminderId = reminder.id,
                        title = reminder.title,
                        detail = dueLabel(kmRemaining, daysRemaining),
                        severity = AlertSeverity.Overdue
                    )
                }
                kmRemaining <= 300 || (daysRemaining != null && daysRemaining <= 7) -> {
                    alerts += DashboardAlert(
                        reminderId = reminder.id,
                        title = reminder.title,
                        detail = dueLabel(kmRemaining, daysRemaining),
                        severity = AlertSeverity.DueSoon
                    )
                }
            }
        }

        val lastReadingDate = readings.maxByOrNull { it.dateIso }?.dateIso?.toLocalDateOrNull()
        val staleDays = lastReadingDate?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt() }
        if (staleDays != null && staleDays > 14) {
            alerts += DashboardAlert(
                reminderId = null,
                title = "Odometer update stale",
                detail = "No reading added in $staleDays days",
                severity = AlertSeverity.Info
            )
        }

        return alerts
            .sortedBy { it.severity.ordinal }
            .take(4)
    }

    private fun buildOdometerSnapshot(
        currentOdometerKm: Int,
        readings: List<com.thisismine.myapplication.domain.model.OdometerReading>
    ): OdometerSnapshot {
        val sortedByDate = readings.sortedBy { it.dateIso }
        val lastReading = sortedByDate.lastOrNull()
        val cutoff = LocalDate.now().minusDays(30)

        val inLast30 = sortedByDate.filter { reading ->
            val parsed = reading.dateIso.toLocalDateOrNull() ?: return@filter false
            !parsed.isBefore(cutoff)
        }
        val distance30 = if (inLast30.size >= 2) {
            inLast30.last().odometerKm - inLast30.first().odometerKm
        } else {
            0
        }

        return OdometerSnapshot(
            currentKm = currentOdometerKm,
            lastReadingDateIso = lastReading?.dateIso,
            distanceLast30DaysKm = distance30.coerceAtLeast(0),
            readingCount = readings.size
        )
    }

    private fun buildUpcomingMaintenance(
        currentOdometerKm: Int,
        reminders: List<ServiceReminder>
    ): List<UpcomingMaintenanceItem> {
        val today = LocalDate.now()
        return reminders
            .sortedWith(
                compareBy<ServiceReminder> { it.effectiveDueKm(currentOdometerKm) - currentOdometerKm }
                    .thenBy { reminder ->
                        reminder.triggerAtIsoDate?.toLocalDateOrNull()?.toEpochDay() ?: Long.MAX_VALUE
                    }
            )
            .take(3)
            .map { reminder ->
                val dueKm = reminder.effectiveDueKm(currentOdometerKm)
                val kmRemaining = dueKm - currentOdometerKm
                val daysRemaining = reminder.triggerAtIsoDate
                    ?.toLocalDateOrNull()
                    ?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() }
                UpcomingMaintenanceItem(
                    title = reminder.title,
                    dueText = dueLabel(kmRemaining, daysRemaining)
                )
            }
    }

    private fun buildDueTimeline(
        currentOdometerKm: Int,
        reminders: List<ServiceReminder>
    ): List<DueTimelineItem> {
        val today = LocalDate.now()
        return reminders
            .map { reminder ->
                val dueKm = reminder.effectiveDueKm(currentOdometerKm)
                val kmRemaining = dueKm - currentOdometerKm
                val daysRemaining = reminder.triggerAtIsoDate
                    ?.toLocalDateOrNull()
                    ?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() }
                DueTimelineItem(
                    reminderId = reminder.id,
                    title = reminder.title,
                    detail = dueLabel(kmRemaining, daysRemaining),
                    severity = when {
                        kmRemaining <= 0 || (daysRemaining != null && daysRemaining < 0) -> AlertSeverity.Overdue
                        kmRemaining <= 300 || (daysRemaining != null && daysRemaining <= 7) -> AlertSeverity.DueSoon
                        else -> AlertSeverity.Info
                    }
                )
            }
            .sortedWith(
                compareBy<DueTimelineItem> { it.severity.ordinal }
                    .thenBy { it.detail }
                    .thenBy { it.title }
            )
            .take(5)
    }

    private fun buildMonthlySpend(
        maintenance: List<MaintenanceEntry>,
        fuel: List<FuelEntry>
    ): List<MonthlySpendItem> {
        val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
        val monthKeys = buildList {
            var month = YearMonth.now()
            repeat(3) {
                add(month)
                month = month.minusMonths(1)
            }
        }.sorted()

        return monthKeys.map { key ->
            val maintenanceTotal = maintenance.sumOf { entry ->
                if (entry.dateIso.toYearMonthOrNull() == key) entry.totalCostPhp else 0.0
            }
            val fuelTotal = fuel.sumOf { entry ->
                if (entry.dateIso.toYearMonthOrNull() == key) entry.costPhp else 0.0
            }
            MonthlySpendItem(
                monthLabel = key.format(monthFormatter),
                maintenanceTotalPhp = maintenanceTotal,
                fuelTotalPhp = fuelTotal,
                totalPhp = maintenanceTotal + fuelTotal
            )
        }.filter { it.totalPhp > 0.0 }
    }

    private fun buildRecentActivity(
        readings: List<com.thisismine.myapplication.domain.model.OdometerReading>,
        maintenanceTitles: List<RecentActivityItem>,
        partTitles: List<RecentActivityItem>
    ): List<RecentActivityItem> {
        val readingItems = readings.map {
            RecentActivityItem(
                dateIso = it.dateIso,
                title = "Odometer updated",
                detail = "${it.odometerKm} km"
            )
        }

        return (readingItems + maintenanceTitles + partTitles)
            .sortedByDescending { it.dateIso }
            .take(5)
    }

    private fun buildFuelAnalyticsSnapshot(motorcycleId: String, fuel: List<FuelEntry>): FuelAnalyticsSnapshot {
        val bikeFuel = fuel.filter { it.motorcycleId == motorcycleId }
        val validEntries = bikeFuel.filter { it.liters > 0.0 && it.distanceKm > 0.0 }

        if (validEntries.isEmpty()) {
            return FuelAnalyticsSnapshot()
        }

        val totalDistance = validEntries.sumOf { it.distanceKm }
        val totalLiters = validEntries.sumOf { it.liters }
        val totalCost = validEntries.sumOf { it.costPhp }

        val latestMonthWithData = validEntries
            .mapNotNull { it.dateIso.toYearMonthOrNull() }
            .maxOrNull()

        val currentMonth = latestMonthWithData ?: YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)

        fun monthMetrics(month: YearMonth): Pair<Double, Double>? {
            val monthEntries = validEntries.filter { it.dateIso.toYearMonthOrNull() == month }
            if (monthEntries.isEmpty()) return null
            val monthDistance = monthEntries.sumOf { it.distanceKm }
            val monthLiters = monthEntries.sumOf { it.liters }
            val monthCost = monthEntries.sumOf { it.costPhp }
            if (monthDistance <= 0.0 || monthLiters <= 0.0) return null
            val kmPerLiter = monthDistance / monthLiters
            val costPerKm = monthCost / monthDistance
            return kmPerLiter to costPerKm
        }

        val currentMetrics = monthMetrics(currentMonth)
        val previousMetrics = monthMetrics(previousMonth)

        val efficiencyDeltaPercent = if (currentMetrics != null && previousMetrics != null && previousMetrics.first > 0.0) {
            ((currentMetrics.first - previousMetrics.first) / previousMetrics.first) * 100.0
        } else {
            null
        }

        val costDeltaPercent = if (currentMetrics != null && previousMetrics != null && previousMetrics.second > 0.0) {
            ((currentMetrics.second - previousMetrics.second) / previousMetrics.second) * 100.0
        } else {
            null
        }

        return FuelAnalyticsSnapshot(
            entryCount = bikeFuel.size,
            averageKmPerLiter = if (totalLiters > 0.0) totalDistance / totalLiters else null,
            costPerKm = if (totalDistance > 0.0) totalCost / totalDistance else null,
            lastEntryDateIso = bikeFuel.maxByOrNull { it.dateIso }?.dateIso,
            currentMonthKmPerLiter = currentMetrics?.first,
            previousMonthKmPerLiter = previousMetrics?.first,
            efficiencyDeltaPercent = efficiencyDeltaPercent,
            currentMonthCostPerKm = currentMetrics?.second,
            previousMonthCostPerKm = previousMetrics?.second,
            costPerKmDeltaPercent = costDeltaPercent
        )
    }

    private fun dueLabel(kmRemaining: Int, daysRemaining: Int?): String {
        val kmText = when {
            kmRemaining < 0 -> "overdue by ${-kmRemaining} km"
            kmRemaining == 0 -> "due now"
            else -> "due in $kmRemaining km"
        }
        val dayText = when {
            daysRemaining == null -> null
            daysRemaining < 0 -> "overdue by ${-daysRemaining} days"
            daysRemaining == 0 -> "due today"
            else -> "due in $daysRemaining days"
        }
        return if (dayText == null) kmText else "$kmText, $dayText"
    }

    private fun applyReminderAction(reminderId: String, action: ReminderActionType): Boolean {
        val reminder = repository.getReminders().firstOrNull { it.id == reminderId } ?: run {
            _uiState.value = _uiState.value.copy(error = "Reminder not found.", message = null)
            return false
        }
        val currentKm = repository.getProfile().currentOdometerKm
        val today = LocalDate.now()
        val result = ReminderActionEngine.apply(action, reminder, currentKm, today)

        if (action == ReminderActionType.MarkDone) {
            repository.addMaintenanceEntry(
                title = reminder.title,
                dateIso = today.toString(),
                odometerKm = currentKm,
                totalCostPhp = 0.0,
                notes = "Auto-logged from reminder completion (${reminder.id})",
                costLevelOverride = "AUTO",
                imageUris = emptyList()
            )
        }

        if (result.shouldArchive) {
            repository.deleteReminder(reminderId)
            notificationScheduler.cancelReminder(reminderId)
        } else {
            val updated = result.updatedReminder ?: return false
            repository.updateReminder(
                id = updated.id,
                title = updated.title,
                triggerAtKm = updated.triggerAtKm,
                triggerAtIsoDate = updated.triggerAtIsoDate,
                repeatingIntervalKm = updated.repeatingIntervalKm,
                lastTriggeredAtKm = updated.lastTriggeredAtKm
            )
        }

        notificationScheduler.syncReminders(repository.getReminders(), currentKm)
        val message = if (action == ReminderActionType.MarkDone) {
            "${result.message}. Logged to service history."
        } else {
            result.message
        }
        refresh(message = message)
        return true
    }
}

private fun ServiceReminder.effectiveDueKm(currentOdometerKm: Int): Int {
    val interval = repeatingIntervalKm ?: return triggerAtKm
    val base = lastTriggeredAtKm ?: triggerAtKm
    if (currentOdometerKm < base + interval) return base + interval
    val cycles = ((currentOdometerKm - base) / interval) + 1
    return base + (cycles * interval)
}

internal fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}

private fun String.toYearMonthOrNull(): YearMonth? {
    return runCatching { YearMonth.from(LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)) }.getOrNull()
}
