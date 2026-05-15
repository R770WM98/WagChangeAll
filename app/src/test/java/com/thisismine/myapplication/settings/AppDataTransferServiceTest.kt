package com.thisismine.myapplication.settings

import com.thisismine.myapplication.data.repository.InMemoryWagChangeRepository
import com.thisismine.myapplication.core.ui.CardDensityMode
import com.thisismine.myapplication.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataTransferServiceTest {

    @Test
    fun backupJson_roundTripsSnapshot() {
        val repository = InMemoryWagChangeRepository()
        repository.addMotorcycle(
            name = "Tourer",
            plateNumber = "XYZ-9999",
            vin = "VIN-TOURER-123",
            purchaseDateIso = "2026-01-01",
            currentOdometerKm = 3210,
            notes = "Weekend bike"
        )
        val service = AppDataTransferService()

        val exported = repository.exportBackupSnapshot()
        val json = service.toBackupJson(exported)
        val restored = service.fromBackupJson(json)

        assertEquals(exported.activeMotorcycleId, restored.snapshot.activeMotorcycleId)
        assertEquals(exported.motorcycles.size, restored.snapshot.motorcycles.size)
        assertTrue(json.contains("\"schemaVersion\""))
    }

    @Test
    fun backupJson_roundTripsSettingsWithoutGeminiSecret() {
        val repository = InMemoryWagChangeRepository()
        repository.addMotorcycle(
            name = "Tourer",
            plateNumber = "XYZ-9999",
            vin = "VIN-TOURER-123",
            purchaseDateIso = "2026-01-01",
            currentOdometerKm = 3210,
            notes = "Weekend bike"
        )
        val service = AppDataTransferService()
        val settings = AppSettings(
            themeMode = ThemeMode.Dark,
            densityMode = CardDensityMode.Compact,
            showActiveBikeBadge = false,
            reduceMotion = true,
            geminiEnabled = true,
            redactSensitiveDataForAi = false
        )

        val json = service.toBackupJson(repository.exportBackupSnapshot(), settings)
        val restored = service.fromBackupJson(json)

        assertEquals(settings, restored.settings)
        assertFalse(json.contains("gemini_api_key"))
        assertFalse(json.contains("app_gemini_secret"))
    }

    @Test
    fun csvExports_includeHeaders() {
        val repository = InMemoryWagChangeRepository()

        val maintenanceCsv = repository.exportMaintenanceCsv()
        val fuelCsv = repository.exportFuelCsv()

        assertTrue(maintenanceCsv.startsWith("motorcycleId,motorcycleName"))
        assertTrue(fuelCsv.startsWith("motorcycleId,motorcycleName"))
    }

    @Test
    fun backupJson_normalizesMissingActiveMotorcycleIdToFirstBike() {
        val service = AppDataTransferService()
        val json = """
            {
              "schemaVersion": 3,
              "activeMotorcycleId": "missing-bike",
              "motorcycles": [
                {
                  "id": "bike-a",
                  "name": "Bike A",
                  "plateNumber": "AAA-1111",
                  "vin": "VIN-A",
                  "purchaseDateIso": "2026-01-01",
                  "currentOdometerKm": 1000,
                  "notes": "",
                  "imageUris": []
                },
                {
                  "id": "bike-b",
                  "name": "Bike B",
                  "plateNumber": "BBB-2222",
                  "vin": "VIN-B",
                  "purchaseDateIso": "2026-02-01",
                  "currentOdometerKm": 2000,
                  "notes": "",
                  "imageUris": []
                }
              ],
              "odometerReadings": [],
              "maintenanceEntries": [],
              "partReplacements": [],
              "fuelEntries": [],
              "reminders": []
            }
        """.trimIndent()

        val restored = service.fromBackupJson(json)

        assertEquals("bike-a", restored.snapshot.activeMotorcycleId)
        assertEquals(2, restored.snapshot.motorcycles.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun backupJson_rejectsUnknownReferencedMotorcycle() {
        val service = AppDataTransferService()
        val json = """
            {
              "schemaVersion": 3,
              "activeMotorcycleId": "bike-a",
              "motorcycles": [
                {
                  "id": "bike-a",
                  "name": "Bike A",
                  "plateNumber": "AAA-1111",
                  "vin": "VIN-A",
                  "purchaseDateIso": "2026-01-01",
                  "currentOdometerKm": 1000,
                  "notes": "",
                  "imageUris": []
                }
              ],
              "odometerReadings": [
                {
                  "id": "od-1",
                  "motorcycleId": "bike-missing",
                  "dateIso": "2026-04-01",
                  "odometerKm": 1020
                }
              ],
              "maintenanceEntries": [],
              "partReplacements": [],
              "fuelEntries": [],
              "reminders": []
            }
        """.trimIndent()

        service.fromBackupJson(json)
    }

    @Test
    fun backupJson_sanitizesInvalidReminderFields() {
        val service = AppDataTransferService()
        val json = """
            {
              "schemaVersion": 3,
              "activeMotorcycleId": "bike-a",
              "motorcycles": [
                {
                  "id": "bike-a",
                  "name": "Bike A",
                  "plateNumber": "AAA-1111",
                  "vin": "VIN-A",
                  "purchaseDateIso": "2026-01-01",
                  "currentOdometerKm": 1000,
                  "notes": "",
                  "imageUris": []
                }
              ],
              "odometerReadings": [],
              "maintenanceEntries": [],
              "partReplacements": [],
              "fuelEntries": [],
              "reminders": [
                {
                  "id": "r1",
                  "motorcycleId": "bike-a",
                  "title": "Oil Change",
                  "triggerAtKm": 1500,
                  "triggerAtIsoDate": "bad-date",
                  "repeatingIntervalKm": 0,
                  "lastTriggeredAtKm": -10
                }
              ]
            }
        """.trimIndent()

        val restored = service.fromBackupJson(json)
        val reminder = restored.snapshot.reminders.first()

        assertEquals("bad-date", reminder.triggerAtIsoDate)
        assertEquals(0, reminder.repeatingIntervalKm)
        assertEquals(-10, reminder.lastTriggeredAtKm)
    }
}
