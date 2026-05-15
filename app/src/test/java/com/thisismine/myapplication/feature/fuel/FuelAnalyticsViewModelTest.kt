package com.thisismine.myapplication.feature.fuel

import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.feature.fuel.data.FuelAnalyticsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelAnalyticsViewModelTest {

    @Test
    fun refresh_computesAverageAndCostPerKm() {
        val vm = FuelAnalyticsViewModel(
            repository = FakeFuelAnalyticsRepository(
                mutableListOf(
                    FuelEntry("f1", "bike-1", "2026-03-10", liters = 5.0, costPhp = 350.0, distanceKm = 180.0),
                    FuelEntry("f2", "bike-1", "2026-03-20", liters = 4.0, costPhp = 300.0, distanceKm = 140.0)
                )
            )
        )

        assertEquals(2, vm.uiState.value.entryCount)
        assertEquals(35.555, vm.uiState.value.averageKmPerLiter!!, 0.01)
        assertEquals(2.031, vm.uiState.value.costPerKm!!, 0.01)
    }

    @Test
    fun refresh_handlesEmptyData() {
        val vm = FuelAnalyticsViewModel(repository = FakeFuelAnalyticsRepository(mutableListOf()))

        assertEquals(0, vm.uiState.value.entryCount)
        assertNull(vm.uiState.value.averageKmPerLiter)
        assertNull(vm.uiState.value.costPerKm)
        assertTrue(vm.uiState.value.monthlyFuelSpend.isEmpty())
    }

    @Test
    fun refresh_buildsMonthlySpendTrend() {
        val vm = FuelAnalyticsViewModel(
            repository = FakeFuelAnalyticsRepository(
                mutableListOf(
                    FuelEntry("f1", "bike-1", "2026-03-10", liters = 5.0, costPhp = 350.0, distanceKm = 180.0),
                    FuelEntry("f2", "bike-1", "2026-03-20", liters = 4.0, costPhp = 300.0, distanceKm = 140.0),
                    FuelEntry("f3", "bike-1", "2026-02-05", liters = 3.0, costPhp = 210.0, distanceKm = 100.0)
                )
            )
        )

        val spend = vm.uiState.value.monthlyFuelSpend
        assertEquals(2, spend.size)
        assertEquals("Mar 2026", spend[0].monthLabel)
        assertEquals(650.0, spend[0].totalCostPhp, 0.01)
        assertEquals("Feb 2026", spend[1].monthLabel)
    }

    @Test
    fun saveFuelEntry_addsEntryAndUpdatesState() {
        val vm = FuelAnalyticsViewModel(repository = FakeFuelAnalyticsRepository(mutableListOf()))

        val ok = vm.saveFuelEntry(
            editingId = null,
            dateIso = "2026-04-07",
            litersInput = "4.2",
            costInput = "300",
            distanceInput = "145"
        )

        assertTrue(ok)
        assertEquals(1, vm.uiState.value.entryCount)
        assertTrue(vm.uiState.value.entries.any { it.dateIso == "2026-04-07" })
    }

    @Test
    fun rollingEfficiency_usesLatestFiveEntries() {
        val entries = (1..6).map {
            FuelEntry(
                id = "f$it",
                motorcycleId = "bike-1",
                dateIso = "2026-03-${10 + it}",
                liters = 5.0,
                costPhp = 300.0,
                distanceKm = 100.0 + it
            )
        }.toMutableList()
        val vm = FuelAnalyticsViewModel(repository = FakeFuelAnalyticsRepository(entries))

        assertEquals(5, vm.uiState.value.rollingEfficiencyKmPerLiter.size)
    }

    @Test
    fun perBikeComparison_aggregatesByMotorcycle() {
        val repository = FakeFuelAnalyticsRepository(
            fuelEntries = mutableListOf(
                FuelEntry("f1", "bike-1", "2026-03-10", liters = 5.0, costPhp = 350.0, distanceKm = 180.0),
                FuelEntry("f2", "bike-2", "2026-03-11", liters = 4.0, costPhp = 320.0, distanceKm = 120.0),
                FuelEntry("f3", "bike-2", "2026-03-12", liters = 3.0, costPhp = 240.0, distanceKm = 90.0)
            ),
            motorcycles = listOf(
                com.thisismine.myapplication.domain.model.MotorcycleProfile("bike-1", "Alpha", "", "", "2025-01-01", 10000, ""),
                com.thisismine.myapplication.domain.model.MotorcycleProfile("bike-2", "Bravo", "", "", "2025-01-01", 11000, "")
            )
        )
        val vm = FuelAnalyticsViewModel(repository)

        val comparison = vm.uiState.value.perBikeComparison
        assertEquals(2, comparison.size)
        assertEquals("Alpha", comparison[0].motorcycleName)
        assertEquals("Bravo", comparison[1].motorcycleName)
        assertEquals(2, comparison[1].entryCount)
    }
}

private class FakeFuelAnalyticsRepository(
    private val fuelEntries: MutableList<FuelEntry>,
    private val motorcycles: List<com.thisismine.myapplication.domain.model.MotorcycleProfile> = listOf(
        com.thisismine.myapplication.domain.model.MotorcycleProfile("bike-1", "Bike 1", "", "", "2025-01-01", 15420, "")
    )
) : FuelAnalyticsRepository {
    override fun getCurrentOdometerKm(): Int = 15420

    override fun getFuelEntries(): List<FuelEntry> = fuelEntries.filter { it.motorcycleId == motorcycles.first().id }

    override fun getAllFuelEntries(): List<FuelEntry> = fuelEntries.toList()

    override fun getMotorcycles(): List<com.thisismine.myapplication.domain.model.MotorcycleProfile> = motorcycles

    override fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        fuelEntries.add(FuelEntry("f-${fuelEntries.size + 1}", "bike-1", dateIso, liters, costPhp, distanceKm))
    }

    override fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        val idx = fuelEntries.indexOfFirst { it.id == id }
        if (idx >= 0) {
            fuelEntries[idx] = fuelEntries[idx].copy(
                dateIso = dateIso,
                liters = liters,
                costPhp = costPhp,
                distanceKm = distanceKm
            )
        }
    }

    override fun deleteFuelEntry(id: String) {
        fuelEntries.removeAll { it.id == id }
    }
}
