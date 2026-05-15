package com.thisismine.myapplication.feature.fuel

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.feature.fuel.data.FuelAnalyticsRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FuelEntryItemUi(
    val id: String,
    val dateIso: String,
    val liters: Double,
    val costPhp: Double,
    val distanceKm: Double,
    val efficiencyKmPerLiter: Double?
)

data class MonthlyFuelSpendItem(
    val monthLabel: String,
    val totalCostPhp: Double
)

data class PerBikeFuelComparisonItem(
    val motorcycleId: String,
    val motorcycleName: String,
    val entryCount: Int,
    val averageKmPerLiter: Double?,
    val costPerKm: Double?
)

data class FuelAnalyticsUiState(
    val currentOdometerKm: Int = 0,
    val entryCount: Int = 0,
    val entries: List<FuelEntryItemUi> = emptyList(),
    val averageKmPerLiter: Double? = null,
    val costPerKm: Double? = null,
    val rollingEfficiencyKmPerLiter: List<Double> = emptyList(),
    val monthlyFuelSpend: List<MonthlyFuelSpendItem> = emptyList(),
    val perBikeComparison: List<PerBikeFuelComparisonItem> = emptyList(),
    val pendingDeleteId: String? = null,
    val error: String? = null,
    val message: String? = null
)

class FuelAnalyticsViewModel(
    private val repository: FuelAnalyticsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FuelAnalyticsUiState())
    val uiState: StateFlow<FuelAnalyticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun saveFuelEntry(
        editingId: String?,
        dateIso: String,
        litersInput: String,
        costInput: String,
        distanceInput: String
    ): Boolean {
        val cleanDate = dateIso.trim().ifBlank { LocalDate.now().toString() }
        val liters = litersInput.trim().toDoubleOrNull()
        val costPhp = costInput.trim().toDoubleOrNull()
        val distanceKm = distanceInput.trim().toDoubleOrNull()

        if (liters == null || costPhp == null || distanceKm == null || liters <= 0.0 || distanceKm < 0.0 || costPhp < 0.0) {
            _uiState.value = _uiState.value.copy(error = "Date, liters, cost, and distance are required.", message = null)
            return false
        }
        if (runCatching { LocalDate.parse(cleanDate, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() == null) {
            _uiState.value = _uiState.value.copy(error = "Date must be yyyy-MM-dd.", message = null)
            return false
        }

        if (editingId == null) {
            repository.addFuelEntry(cleanDate, liters, costPhp, distanceKm)
            refresh(message = "Fuel entry added.")
        } else {
            repository.updateFuelEntry(editingId, cleanDate, liters, costPhp, distanceKm)
            refresh(message = "Fuel entry updated.")
        }
        return true
    }

    fun requestDeleteFuelEntry(id: String) {
        _uiState.value = _uiState.value.copy(pendingDeleteId = id)
    }

    fun cancelDeleteFuelEntry() {
        _uiState.value = _uiState.value.copy(pendingDeleteId = null)
    }

    fun confirmDeleteFuelEntry() {
        val pendingId = _uiState.value.pendingDeleteId ?: return
        repository.deleteFuelEntry(pendingId)
        _uiState.value = _uiState.value.copy(pendingDeleteId = null)
        refresh(message = "Fuel entry deleted.")
    }

    fun refresh(message: String? = _uiState.value.message) {
        val result = runCatching {
            val entries = repository.getFuelEntries()
            val allEntries = repository.getAllFuelEntries()
            val validEfficiencyEntries = entries.filter { it.liters > 0.0 && it.distanceKm > 0.0 }
            val totalDistanceKm = validEfficiencyEntries.sumOf { it.distanceKm }
            val totalLiters = validEfficiencyEntries.sumOf { it.liters }
            val averageKmPerLiter = if (totalLiters > 0.0) totalDistanceKm / totalLiters else null
            val totalCost = validEfficiencyEntries.sumOf { it.costPhp }
            val costPerKm = if (totalDistanceKm > 0.0) totalCost / totalDistanceKm else null

            _uiState.value = _uiState.value.copy(
                currentOdometerKm = runCatching { repository.getCurrentOdometerKm() }.getOrDefault(0),
                entryCount = entries.size,
                entries = entries
                    .sortedByDescending { it.dateIso }
                    .map {
                        FuelEntryItemUi(
                            id = it.id,
                            dateIso = it.dateIso,
                            liters = it.liters,
                            costPhp = it.costPhp,
                            distanceKm = it.distanceKm,
                            efficiencyKmPerLiter = if (it.liters > 0.0) it.distanceKm / it.liters else null
                        )
                    },
                averageKmPerLiter = averageKmPerLiter,
                costPerKm = costPerKm,
                rollingEfficiencyKmPerLiter = buildRollingEfficiency(validEfficiencyEntries),
                monthlyFuelSpend = buildMonthlyFuelSpend(entries),
                perBikeComparison = buildPerBikeComparison(allEntries),
                error = null,
                message = message
            )
        }

        if (result.isFailure) {
            _uiState.value = _uiState.value.copy(
                currentOdometerKm = 0,
                entryCount = 0,
                entries = emptyList(),
                averageKmPerLiter = null,
                costPerKm = null,
                rollingEfficiencyKmPerLiter = emptyList(),
                monthlyFuelSpend = emptyList(),
                perBikeComparison = emptyList(),
                error = "Unable to load fuel data. Please review imported dates and active motorcycle.",
                message = null
            )
        }
    }

    private fun buildRollingEfficiency(entries: List<FuelEntry>): List<Double> {
        return entries
            .sortedByDescending { it.dateIso }
            .take(5)
            .map { it.distanceKm / it.liters }
    }

    private fun buildMonthlyFuelSpend(entries: List<FuelEntry>): List<MonthlyFuelSpendItem> {
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
        return entries
            .groupBy { entry ->
                runCatching { YearMonth.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(entry.dateIso)) }.getOrNull()
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .toList()
            .sortedByDescending { (month, _) -> month }
            .map { (month, monthEntries) ->
                MonthlyFuelSpendItem(
                    monthLabel = month.format(formatter),
                    totalCostPhp = monthEntries.sumOf { it.costPhp }
                )
            }
    }

    private fun buildPerBikeComparison(entries: List<FuelEntry>): List<PerBikeFuelComparisonItem> {
        val motorcycles = repository.getMotorcycles().associateBy { it.id }
        return entries
            .groupBy { it.motorcycleId }
            .map { (motorcycleId, bikeEntries) ->
                val valid = bikeEntries.filter { it.liters > 0.0 && it.distanceKm > 0.0 }
                val totalDistance = valid.sumOf { it.distanceKm }
                val totalLiters = valid.sumOf { it.liters }
                val totalCost = valid.sumOf { it.costPhp }
                PerBikeFuelComparisonItem(
                    motorcycleId = motorcycleId,
                    motorcycleName = motorcycles[motorcycleId]?.name ?: "Unknown",
                    entryCount = bikeEntries.size,
                    averageKmPerLiter = if (totalLiters > 0.0) totalDistance / totalLiters else null,
                    costPerKm = if (totalDistance > 0.0) totalCost / totalDistance else null
                )
            }
            .sortedBy { it.motorcycleName.lowercase(Locale.US) }
    }
}
