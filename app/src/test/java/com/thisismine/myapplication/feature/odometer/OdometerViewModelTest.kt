package com.thisismine.myapplication.feature.odometer

import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.feature.odometer.data.OdometerRepository
import com.thisismine.myapplication.feature.odometer.location.GeoPoint
import com.thisismine.myapplication.feature.odometer.location.TripLocationClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerViewModelTest {

    @Test
    fun saveReading_withLowerValue_setsValidationError() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        viewModel.onReadingInputChange("900")
        viewModel.saveReading()

        val state = viewModel.uiState.value
        assertEquals("Reading must be equal to or higher than current odometer.", state.inputError)
        assertEquals(1000, state.currentOdometerKm)
        assertEquals(1, state.readings.size)
    }

    @Test
    fun saveReading_withValidValue_updatesCurrentAndHistory() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        viewModel.onReadingInputChange("1200")
        viewModel.saveReading()

        val state = viewModel.uiState.value
        assertEquals(null, state.inputError)
        assertEquals(1200, state.currentOdometerKm)
        assertEquals("", state.newReadingInput)
        assertEquals(2, state.readings.size)
        assertEquals(1200, state.readings.first().odometerKm)
    }

    @Test
    fun saveReading_withNonNumericValue_setsValidationError() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        viewModel.onReadingInputChange("abc")
        viewModel.saveReading()

        val state = viewModel.uiState.value
        assertEquals("Enter a valid kilometer value.", state.inputError)
        assertTrue(state.readings.isNotEmpty())
        assertEquals(1000, state.currentOdometerKm)
    }

    @Test
    fun addMotorcycle_thenTrackReading_onlyAffectsNewMotorcycle() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        val added = viewModel.addMotorcycle(
            name = "Weekend Ride",
            plateNumber = "ABC-1111",
            vin = "VIN-001",
            purchaseDateIso = "2024-05-01",
            currentOdometerInput = "500",
            notes = ""
        )
        assertTrue(added)

        viewModel.onReadingInputChange("650")
        viewModel.saveReading()

        viewModel.selectMotorcycle("bike-1")
        assertEquals(1000, viewModel.uiState.value.currentOdometerKm)

        val weekendId = viewModel.uiState.value.motorcycles.first { it.name == "Weekend Ride" }.id
        viewModel.selectMotorcycle(weekendId)
        assertEquals(650, viewModel.uiState.value.currentOdometerKm)
    }

    @Test
    fun updateActiveMotorcycle_updatesSelectedBikeProfile() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        val updated = viewModel.updateActiveMotorcycle(
            name = "Daily Ride Updated",
            plateNumber = "ABC-9999",
            vin = "VIN-UPDATED",
            purchaseDateIso = "2025-06-15",
            currentOdometerInput = "1100",
            notes = "New notes"
        )

        assertTrue(updated)
        val state = viewModel.uiState.value
        assertEquals("Daily Ride Updated", state.motorcycles.first().name)
        assertEquals("ABC-9999", state.motorcycles.first().plateNumber)
        assertEquals(1100, state.currentOdometerKm)
    }

    @Test
    fun updateActiveMotorcycle_trimsProfileInput() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val viewModel = OdometerViewModel(repo)

        val updated = viewModel.updateActiveMotorcycle(
            name = "  Daily Ride Updated  ",
            plateNumber = "  ABC-9999  ",
            vin = "  VIN-UPDATED  ",
            purchaseDateIso = " 2025-06-15 ",
            currentOdometerInput = " 1100 ",
            notes = "  New notes  "
        )

        assertTrue(updated)
        val bike = viewModel.uiState.value.motorcycles.first()
        assertEquals("Daily Ride Updated", bike.name)
        assertEquals("ABC-9999", bike.plateNumber)
        assertEquals("VIN-UPDATED", bike.vin)
        assertEquals("2025-06-15", bike.purchaseDateIso)
        assertEquals("New notes", bike.notes)
    }

    @Test
    fun gpsTrip_applyTrackedDistance_updatesOdometer() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val tripClient = FakeTripLocationClient()
        val viewModel = OdometerViewModel(repo, tripClient)

        viewModel.startTripTracking()
        tripClient.pushPoint(GeoPoint(14.5995, 120.9842, 5f))
        tripClient.pushPoint(GeoPoint(14.6030, 120.9842, 5f))
        viewModel.applyTrackedDistanceToOdometer()

        val state = viewModel.uiState.value
        assertTrue(state.currentOdometerKm > 1000)
        assertEquals(0.0, state.trackedDistanceKm, 0.0001)
        assertEquals(false, state.isTripTracking)
    }

    @Test
    fun gpsTrip_pauseAndResume_keepsDistanceAndContinuesTracking() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val tripClient = FakeTripLocationClient()
        val viewModel = OdometerViewModel(repo, tripClient)

        viewModel.startTripTracking()
        tripClient.pushPoint(GeoPoint(14.5995, 120.9842, 5f))
        tripClient.pushPoint(GeoPoint(14.6000, 120.9842, 5f))
        val beforePause = viewModel.uiState.value.trackedDistanceKm

        viewModel.pauseTripTracking()
        assertTrue(viewModel.uiState.value.isTripPaused)
        tripClient.pushPoint(GeoPoint(14.6005, 120.9842, 5f))
        assertEquals(beforePause, viewModel.uiState.value.trackedDistanceKm, 0.0001)

        viewModel.resumeTripTracking()
        assertFalse(viewModel.uiState.value.isTripPaused)
        tripClient.pushPoint(GeoPoint(14.6005, 120.9842, 5f))
        tripClient.pushPoint(GeoPoint(14.6010, 120.9842, 5f))

        assertTrue(viewModel.uiState.value.trackedDistanceKm > beforePause)
    }

    @Test
    fun gpsTrip_driftFiltering_ignoresTinyAndLargeJumps() {
        val repo = FakeOdometerRepository(initialKm = 1000)
        val tripClient = FakeTripLocationClient()
        val viewModel = OdometerViewModel(repo, tripClient)

        viewModel.startTripTracking()
        tripClient.pushPoint(GeoPoint(14.5995, 120.9842, 5f))
        tripClient.pushPoint(GeoPoint(14.59951, 120.9842, 5f))
        tripClient.pushPoint(GeoPoint(14.6200, 120.9842, 5f))

        assertEquals(0.0, viewModel.uiState.value.trackedDistanceKm, 0.0001)
    }
}

private class FakeOdometerRepository(initialKm: Int) : OdometerRepository {
    private val motorcycles = mutableListOf(
        MotorcycleProfile(
            id = "bike-1",
            name = "Daily Ride",
            plateNumber = "ABC-1234",
            vin = "JYARN23E1PA123456",
            purchaseDateIso = "2025-05-10",
            currentOdometerKm = initialKm,
            notes = "Main city bike"
        )
    )
    private var activeMotorcycleId = "bike-1"
    private val readings = mutableListOf(
        OdometerReading(
            id = "seed",
            motorcycleId = "bike-1",
            dateIso = "2026-03-01",
            odometerKm = initialKm
        )
    )

    override fun getCurrentOdometerKm(): Int =
        motorcycles.first { it.id == activeMotorcycleId }.currentOdometerKm

    override fun getReadings(): List<OdometerReading> =
        readings.filter { it.motorcycleId == activeMotorcycleId }

    override fun getMotorcycles(): List<MotorcycleProfile> = motorcycles.toList()

    override fun getActiveMotorcycleId(): String = activeMotorcycleId

    override fun setActiveMotorcycle(motorcycleId: String) {
        if (motorcycles.any { it.id == motorcycleId }) {
            activeMotorcycleId = motorcycleId
        }
    }

    override fun addMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String,
        imageUris: List<String>
    ) {
        val id = "bike-${motorcycles.size + 1}"
        motorcycles.add(
            MotorcycleProfile(
                id = id,
                name = name,
                plateNumber = plateNumber,
                vin = vin,
                purchaseDateIso = purchaseDateIso,
                currentOdometerKm = currentOdometerKm,
                notes = notes,
                imageUris = imageUris
            )
        )
        readings.add(
            OdometerReading(
                id = "seed-${readings.size + 1}",
                motorcycleId = id,
                dateIso = "2026-04-01",
                odometerKm = currentOdometerKm
            )
        )
        activeMotorcycleId = id
    }

    override fun updateMotorcycle(
        motorcycleId: String,
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String
    ) {
        val idx = motorcycles.indexOfFirst { it.id == motorcycleId }
        if (idx >= 0) {
            motorcycles[idx] = motorcycles[idx].copy(
                name = name,
                plateNumber = plateNumber,
                vin = vin,
                purchaseDateIso = purchaseDateIso,
                currentOdometerKm = currentOdometerKm,
                notes = notes
            )
        }
    }

    override fun addReading(odometerKm: Int) {
        val index = motorcycles.indexOfFirst { it.id == activeMotorcycleId }
        if (index >= 0) {
            motorcycles[index] = motorcycles[index].copy(currentOdometerKm = odometerKm)
        }
        readings.add(
            OdometerReading(
                id = "new-${readings.size + 1}",
                motorcycleId = activeMotorcycleId,
                dateIso = "2026-03-31",
                odometerKm = odometerKm
            )
        )
    }
}

private class FakeTripLocationClient : TripLocationClient {
    private var onPoint: ((GeoPoint) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override fun startTracking(onPoint: (GeoPoint) -> Unit, onError: (String) -> Unit) {
        this.onPoint = onPoint
        this.onError = onError
    }

    override fun stopTracking() {
        onPoint = null
        onError = null
    }

    fun pushPoint(point: GeoPoint) {
        onPoint?.invoke(point)
    }
}
