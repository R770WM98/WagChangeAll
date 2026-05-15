package com.thisismine.myapplication.feature.odometer

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.core.validation.MotorcycleProfileValidationResult
import com.thisismine.myapplication.core.validation.validateMotorcycleProfileInput
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.feature.odometer.data.OdometerRepository
import com.thisismine.myapplication.feature.odometer.location.GeoPoint
import com.thisismine.myapplication.feature.odometer.location.NoOpTripLocationClient
import com.thisismine.myapplication.feature.odometer.location.TripLocationClient
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OdometerUiState(
    val motorcycles: List<MotorcycleProfile> = emptyList(),
    val activeMotorcycleId: String = "",
    val currentOdometerKm: Int = 0,
    val readings: List<OdometerReading> = emptyList(),
    val newReadingInput: String = "",
    val inputError: String? = null,
    val motorcycleFormError: String? = null,
    val motorcycleFormMessage: String? = null,
    val isTripTracking: Boolean = false,
    val isTripPaused: Boolean = false,
    val trackedDistanceKm: Double = 0.0,
    val tripError: String? = null
)

class OdometerViewModel(
    private val repository: OdometerRepository,
    private val tripLocationClient: TripLocationClient = NoOpTripLocationClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(OdometerUiState())
    val uiState: StateFlow<OdometerUiState> = _uiState.asStateFlow()

    private var lastPoint: GeoPoint? = null

    companion object {
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 20f
        private const val MIN_MOVEMENT_KM = 0.008
        private const val MAX_MOVEMENT_KM = 0.200
        private const val SMOOTHING_ALPHA = 0.35
    }

    init {
        refreshState()
    }

    fun onReadingInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            newReadingInput = value,
            inputError = null
        )
    }

    fun selectMotorcycle(motorcycleId: String) {
        if (motorcycleId == _uiState.value.activeMotorcycleId) return

        if (_uiState.value.isTripTracking) {
            stopTripTracking()
            _uiState.value = _uiState.value.copy(trackedDistanceKm = 0.0)
        }

        repository.setActiveMotorcycle(motorcycleId)
        lastPoint = null
        refreshState()
    }

    fun addMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerInput: String,
        notes: String
    ): Boolean {
        val validation = validateMotorcycleProfileInput(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometerInput = currentOdometerInput,
            notes = notes,
            invalidOdometerMessage = "Enter a valid starting odometer."
        )
        if (validation is MotorcycleProfileValidationResult.Error) {
            _uiState.value = _uiState.value.copy(
                motorcycleFormError = validation.message,
                motorcycleFormMessage = null
            )
            return false
        }
        val value = (validation as MotorcycleProfileValidationResult.Success).value

        repository.addMotorcycle(
            name = value.name,
            plateNumber = value.plateNumber,
            vin = value.vin,
            purchaseDateIso = value.purchaseDateIso,
            currentOdometerKm = value.odometerKm,
            notes = value.notes
        )
        refreshState()
        _uiState.value = _uiState.value.copy(
            motorcycleFormError = null,
            motorcycleFormMessage = "Motorcycle added and selected."
        )
        return true
    }

    fun updateActiveMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerInput: String,
        notes: String
    ): Boolean {
        val activeId = _uiState.value.activeMotorcycleId
        if (activeId.isBlank()) return false

        val validation = validateMotorcycleProfileInput(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometerInput = currentOdometerInput,
            notes = notes,
            invalidOdometerMessage = "Enter a valid odometer."
        )
        if (validation is MotorcycleProfileValidationResult.Error) {
            _uiState.value = _uiState.value.copy(
                motorcycleFormError = validation.message,
                motorcycleFormMessage = null
            )
            return false
        }
        val value = (validation as MotorcycleProfileValidationResult.Success).value

        repository.updateMotorcycle(
            motorcycleId = activeId,
            name = value.name,
            plateNumber = value.plateNumber,
            vin = value.vin,
            purchaseDateIso = value.purchaseDateIso,
            currentOdometerKm = value.odometerKm,
            notes = value.notes
        )
        refreshState()
        _uiState.value = _uiState.value.copy(
            motorcycleFormError = null,
            motorcycleFormMessage = "Active motorcycle updated."
        )
        return true
    }

    fun saveReading() {
        val rawInput = _uiState.value.newReadingInput.trim()
        val parsedValue = rawInput.toIntOrNull()

        if (parsedValue == null) {
            _uiState.value = _uiState.value.copy(inputError = "Enter a valid kilometer value.")
            return
        }

        if (parsedValue < _uiState.value.currentOdometerKm) {
            _uiState.value = _uiState.value.copy(
                inputError = "Reading must be equal to or higher than current odometer."
            )
            return
        }

        repository.addReading(parsedValue)
        refreshState()
        _uiState.value = _uiState.value.copy(newReadingInput = "", inputError = null)
    }

    fun startTripTracking() {
        if (_uiState.value.isTripTracking) return

        _uiState.value = _uiState.value.copy(
            isTripTracking = true,
            isTripPaused = false,
            trackedDistanceKm = 0.0,
            tripError = null
        )
        lastPoint = null
        beginLocationUpdates()
    }

    fun pauseTripTracking() {
        if (!_uiState.value.isTripTracking || _uiState.value.isTripPaused) return
        tripLocationClient.stopTracking()
        _uiState.value = _uiState.value.copy(isTripPaused = true)
    }

    fun resumeTripTracking() {
        if (!_uiState.value.isTripTracking || !_uiState.value.isTripPaused) return
        _uiState.value = _uiState.value.copy(isTripPaused = false, tripError = null)
        lastPoint = null
        beginLocationUpdates()
    }

    fun stopTripTracking() {
        if (!_uiState.value.isTripTracking) return
        tripLocationClient.stopTracking()
        _uiState.value = _uiState.value.copy(isTripTracking = false, isTripPaused = false)
    }

    fun applyTrackedDistanceToOdometer() {
        stopTripTracking()
        val tracked = _uiState.value.trackedDistanceKm
        if (tracked < 0.1) {
            _uiState.value = _uiState.value.copy(tripError = "Tracked distance is too small to apply.")
            return
        }

        val addKm = tracked.roundToInt().coerceAtLeast(1)
        val nextValue = _uiState.value.currentOdometerKm + addKm
        repository.addReading(nextValue)
        refreshState()
        _uiState.value = _uiState.value.copy(
            trackedDistanceKm = 0.0,
            tripError = null,
            inputError = null,
            newReadingInput = ""
        )
    }

    override fun onCleared() {
        tripLocationClient.stopTracking()
        super.onCleared()
    }

    private fun refreshState() {
        val history = repository.getReadings().sortedByDescending { it.odometerKm }
        _uiState.value = _uiState.value.copy(
            motorcycles = repository.getMotorcycles(),
            activeMotorcycleId = repository.getActiveMotorcycleId(),
            currentOdometerKm = repository.getCurrentOdometerKm(),
            readings = history,
            motorcycleFormError = null
        )
    }

    private fun beginLocationUpdates() {
        tripLocationClient.startTracking(
            onPoint = { point ->
                if (point.accuracyMeters > MAX_ACCEPTABLE_ACCURACY_METERS) return@startTracking

                val filteredPoint = smoothPoint(lastPoint, point)
                val previous = lastPoint
                if (previous != null) {
                    val delta = haversineDistanceKm(previous, filteredPoint)
                    if (delta in MIN_MOVEMENT_KM..MAX_MOVEMENT_KM) {
                        _uiState.value = _uiState.value.copy(
                            trackedDistanceKm = _uiState.value.trackedDistanceKm + delta,
                            tripError = null
                        )
                    }
                }
                lastPoint = filteredPoint
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isTripTracking = false,
                    isTripPaused = false,
                    tripError = message
                )
            }
        )
    }

    private fun smoothPoint(previous: GeoPoint?, current: GeoPoint): GeoPoint {
        if (previous == null) return current
        return GeoPoint(
            latitude = previous.latitude + (current.latitude - previous.latitude) * SMOOTHING_ALPHA,
            longitude = previous.longitude + (current.longitude - previous.longitude) * SMOOTHING_ALPHA,
            accuracyMeters = current.accuracyMeters
        )
    }

    private fun haversineDistanceKm(a: GeoPoint, b: GeoPoint): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        return 2 * earthRadiusKm * asin(sqrt(h))
    }
}
