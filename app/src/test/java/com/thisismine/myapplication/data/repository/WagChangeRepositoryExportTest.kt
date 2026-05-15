package com.thisismine.myapplication.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WagChangeRepositoryExportTest {

    @Test
    fun exportMaintenanceCsv_isSortedAndFormatsCost() {
        val repository = InMemoryWagChangeRepository()
        repository.addMaintenanceEntry(
            title = "Chain, Lube",
            dateIso = "2026-01-01",
            odometerKm = 14000,
            totalCostPhp = 1000.0,
            notes = "Includes \"premium\" lube"
        )
        repository.addMaintenanceEntry(
            title = "Brake Check",
            dateIso = "2026-12-01",
            odometerKm = 17000,
            totalCostPhp = 250.5,
            notes = "All good"
        )

        val csv = repository.exportMaintenanceCsv()
        val lines = csv.lines().filter { it.isNotBlank() }

        assertTrue(lines.first().startsWith("motorcycleId,motorcycleName"))
        assertEquals(4, lines.size) // header + seeded + 2 new
        assertTrue(lines[1].contains("2026-01-01"))
        assertTrue(csv.contains("1000.00"))
        assertTrue(csv.contains("\"Chain, Lube\""))
        assertTrue(csv.contains("\"Includes \"\"premium\"\" lube\""))
    }

    @Test
    fun exportFuelCsv_isSortedAndFormatsDecimals() {
        val repository = InMemoryWagChangeRepository()
        repository.addFuelEntry(dateIso = "2026-01-02", liters = 3.5, costPhp = 200.0, distanceKm = 120.0)
        repository.addFuelEntry(dateIso = "2026-12-05", liters = 5.25, costPhp = 340.75, distanceKm = 188.4)

        val csv = repository.exportFuelCsv()
        val lines = csv.lines().filter { it.isNotBlank() }

        assertTrue(lines.first().startsWith("motorcycleId,motorcycleName,entryId,dateIso"))
        assertEquals(4, lines.size) // header + seeded + 2 new
        assertTrue(lines[1].contains("2026-01-02"))
        assertTrue(csv.contains("3.50"))
        assertTrue(csv.contains("340.75"))
        assertTrue(csv.contains("188.40"))
    }
}

