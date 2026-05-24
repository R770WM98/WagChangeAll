package com.thisismine.myapplication.feature.dashboard

import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.PartReplacement
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardPrintHelperTest {

    @Test
    fun serviceHistoryHtml_includesSummaryAndFormattedDates() {
        val html = buildServiceHistoryHtml(
            motorcycleName = "Daily Ride",
            entries = listOf(
                MaintenanceEntry(
                    id = "m1",
                    motorcycleId = "bike-1",
                    title = "Oil Change",
                    dateIso = "2026-04-01",
                    odometerKm = 15000,
                    totalCostPhp = 780.0,
                    notes = "Regular"
                ),
                MaintenanceEntry(
                    id = "m2",
                    motorcycleId = "bike-1",
                    title = "CVT Cleaning",
                    dateIso = "2026-04-10",
                    odometerKm = 15200,
                    totalCostPhp = 650.0,
                    notes = "Detailed"
                )
            )
        )

        assertTrue(html.contains("Generated:"))
        assertTrue(html.contains("Entries: 2"))
        assertTrue(html.contains("Total: PHP 1430.00"))
        assertTrue(html.contains("Apr 01, 2026"))
        assertTrue(html.contains("Apr 10, 2026"))
        assertTrue(html.indexOf("Apr 10, 2026") < html.indexOf("Apr 01, 2026"))
    }

    @Test
    fun fullMaintenanceHtml_includesCategoryAndTotals() {
        val html = buildFullMaintenanceHistoryHtml(
            motorcycleName = "Tourer",
            maintenanceEntries = listOf(
                MaintenanceEntry(
                    id = "m1",
                    motorcycleId = "bike-1",
                    title = "Oil Change",
                    dateIso = "2026-04-01",
                    odometerKm = 15000,
                    totalCostPhp = 780.0,
                    notes = "Regular"
                )
            ),
            partReplacements = listOf(
                PartReplacement(
                    id = "p1",
                    motorcycleId = "bike-1",
                    partName = "Brake Pads",
                    dateIso = "2026-04-03",
                    odometerKm = 15100,
                    totalCostPhp = 450.0,
                    notes = "Front"
                )
            )
        )

        assertTrue(html.contains("Generated:"))
        assertTrue(html.contains("Category"))
        assertTrue(html.contains("Service"))
        assertTrue(html.contains("Part"))
        assertTrue(html.contains("Entries: 2"))
        assertTrue(html.contains("Total: PHP 1230.00"))
    }
}
