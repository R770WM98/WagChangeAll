package com.thisismine.myapplication.feature.motorcycles

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.core.validation.MotorcycleProfileValidationResult
import com.thisismine.myapplication.core.validation.validateMotorcycleProfileInput
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MotorcyclesUiState(
    val motorcycles: List<MotorcycleProfile> = emptyList(),
    val filteredMotorcycles: List<MotorcycleProfile> = emptyList(),
    val activeMotorcycleId: String = "",
    val filterQuery: String = "",
    val pendingDeleteMotorcycleId: String? = null,
    val error: String? = null,
    val message: String? = null
)

class MotorcyclesViewModel(
    private val repository: WagChangeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MotorcyclesUiState())
    val uiState: StateFlow<MotorcyclesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onFilterQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(filterQuery = value)
        refresh()
    }

    fun selectMotorcycle(id: String) {
        repository.setActiveMotorcycle(id)
        refresh(message = "Active motorcycle changed.")
    }

    fun requestDeleteMotorcycle(motorcycleId: String) {
        _uiState.value = _uiState.value.copy(pendingDeleteMotorcycleId = motorcycleId)
    }

    fun cancelDeleteMotorcycle() {
        _uiState.value = _uiState.value.copy(pendingDeleteMotorcycleId = null)
    }

    fun confirmDeleteMotorcycle() {
        val pendingId = _uiState.value.pendingDeleteMotorcycleId ?: return
        _uiState.value = _uiState.value.copy(pendingDeleteMotorcycleId = null)
        repository.deleteMotorcycle(pendingId)
        refresh(message = "Motorcycle deleted.")
    }

    fun addMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        odometerInput: String,
        notes: String,
        imageUris: List<String> = emptyList()
    ): Boolean {
        val validation = validateMotorcycleProfileInput(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometerInput = odometerInput,
            notes = notes,
            invalidOdometerMessage = "Enter a valid starting odometer."
        )
        if (validation is MotorcycleProfileValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = validation.message, message = null)
            return false
        }
        val value = (validation as MotorcycleProfileValidationResult.Success).value

        repository.addMotorcycle(
            name = value.name,
            plateNumber = value.plateNumber,
            vin = value.vin,
            purchaseDateIso = value.purchaseDateIso,
            currentOdometerKm = value.odometerKm,
            notes = value.notes,
            imageUris = imageUris
        )
        refresh(message = "Motorcycle added.")
        return true
    }

    fun updateMotorcycle(
        motorcycleId: String,
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        odometerInput: String,
        notes: String,
        imageUris: List<String>? = null
    ): Boolean {
        val validation = validateMotorcycleProfileInput(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometerInput = odometerInput,
            notes = notes,
            invalidOdometerMessage = "Enter a valid odometer."
        )
        if (validation is MotorcycleProfileValidationResult.Error) {
            _uiState.value = _uiState.value.copy(error = validation.message, message = null)
            return false
        }
        val value = (validation as MotorcycleProfileValidationResult.Success).value

        repository.updateMotorcycle(
            motorcycleId = motorcycleId,
            name = value.name,
            plateNumber = value.plateNumber,
            vin = value.vin,
            purchaseDateIso = value.purchaseDateIso,
            currentOdometerKm = value.odometerKm,
            notes = value.notes
        )
        if (imageUris != null) {
            repository.updateMotorcycleImages(motorcycleId, imageUris)
        }
        refresh(message = "Motorcycle updated.")
        return true
    }

    fun addMotorcyclePhoto(motorcycleId: String, imageUri: String) {
        val target = repository.getMotorcycles().firstOrNull { it.id == motorcycleId } ?: return
        repository.updateMotorcycleImages(motorcycleId, target.imageUris + imageUri)
        refresh(message = "Motorcycle photo added.")
    }

    fun removeMotorcyclePhoto(motorcycleId: String, index: Int) {
        val target = repository.getMotorcycles().firstOrNull { it.id == motorcycleId } ?: return
        if (index !in target.imageUris.indices) return
        repository.updateMotorcycleImages(
            motorcycleId,
            target.imageUris.filterIndexed { i, _ -> i != index }
        )
        refresh(message = "Motorcycle photo removed.")
    }

    private fun refresh(message: String? = _uiState.value.message) {
        val allMotorcycles = repository.getMotorcycles()
        val query = _uiState.value.filterQuery.trim().lowercase()
        val filtered = if (query.isBlank()) {
            allMotorcycles
        } else {
            allMotorcycles.filter { bike ->
                bike.name.lowercase().contains(query) ||
                    bike.plateNumber.lowercase().contains(query) ||
                    bike.vin.lowercase().contains(query) ||
                    bike.purchaseDateIso.contains(query)
            }
        }

        _uiState.value = MotorcyclesUiState(
            motorcycles = allMotorcycles,
            filteredMotorcycles = filtered,
            activeMotorcycleId = repository.getActiveMotorcycleId(),
            filterQuery = _uiState.value.filterQuery,
            pendingDeleteMotorcycleId = _uiState.value.pendingDeleteMotorcycleId,
            error = null,
            message = message
        )
    }
}
