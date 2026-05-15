package com.thisismine.myapplication.feature.maintenance

import androidx.lifecycle.ViewModel
import com.thisismine.myapplication.feature.maintenance.data.MaintenanceRepository
import com.thisismine.myapplication.feature.maintenance.model.PartRecord
import com.thisismine.myapplication.feature.maintenance.model.ServiceRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PartLifecycleSeverity {
    Overdue,
    DueSoon,
    Healthy
}

data class PartLifecycleInsight(
    val partId: String,
    val title: String,
    val detail: String,
    val severity: PartLifecycleSeverity
)

data class MaintenanceUiState(
    val currentOdometerKm: Int = 0,
    val services: List<ServiceRecord> = emptyList(),
    val parts: List<PartRecord> = emptyList(),
    val filteredServices: List<ServiceRecord> = emptyList(),
    val filteredParts: List<PartRecord> = emptyList(),
    val lifecycleInsights: List<PartLifecycleInsight> = emptyList(),
    val overduePartCount: Int = 0,
    val dueSoonPartCount: Int = 0,
    val serviceTemplates: List<String> = emptyList(),
    val serviceFilterQuery: String = "",
    val partFilterQuery: String = "",
    val pendingDeleteServiceId: String? = null,
    val pendingDeletePartId: String? = null,
    val error: String? = null,
    val message: String? = null
)

class MaintenanceViewModel(
    private val repository: MaintenanceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onServiceFilterQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(serviceFilterQuery = value)
        refresh()
    }

    fun onPartFilterQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(partFilterQuery = value)
        refresh()
    }

    fun saveService(
        editingId: String?,
        serviceType: String,
        dateIso: String,
        odometerInput: String,
        costInput: String,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ): Boolean {
        val odometer = odometerInput.trim().toIntOrNull()
        val cost = costInput.trim().toDoubleOrNull()
        val cleanType = serviceType.trim()
        val cleanDate = dateIso.trim().ifBlank { LocalDate.now().toString() }

        if (cleanType.isBlank() || odometer == null || cost == null) {
            _uiState.value = _uiState.value.copy(error = "Service type, odometer, and cost are required.", message = null)
            return false
        }

        if (editingId == null) {
            repository.addServiceRecord(cleanType, cleanDate, odometer, cost, notes.trim(), costLevelOverride, imageUris)
            _uiState.value = _uiState.value.copy(error = null, message = "Service record added.")
        } else {
            repository.updateServiceRecord(editingId, cleanType, cleanDate, odometer, cost, notes.trim(), costLevelOverride, imageUris)
            _uiState.value = _uiState.value.copy(error = null, message = "Service record updated.")
        }
        refresh()
        return true
    }

    fun requestDeleteService(id: String) {
        _uiState.value = _uiState.value.copy(pendingDeleteServiceId = id)
    }

    fun cancelDeleteService() {
        _uiState.value = _uiState.value.copy(pendingDeleteServiceId = null)
    }

    fun confirmDeleteService() {
        val pendingId = _uiState.value.pendingDeleteServiceId ?: return
        _uiState.value = _uiState.value.copy(pendingDeleteServiceId = null)
        repository.deleteServiceRecord(pendingId)
        refresh(message = "Service record deleted.")
    }

    fun savePart(
        editingId: String?,
        partName: String,
        dateIso: String,
        odometerInput: String,
        costInput: String,
        notes: String,
        costLevelOverride: String?,
        replacementIntervalKmInput: String,
        warrantyExpiryIsoInput: String,
        imageUris: List<String>
    ): Boolean {
        val odometer = odometerInput.trim().toIntOrNull()
        val cost = costInput.trim().toDoubleOrNull()
        val cleanName = partName.trim()
        val cleanDate = dateIso.trim().ifBlank { LocalDate.now().toString() }
        val replacementIntervalKm = replacementIntervalKmInput.trim().toIntOrNull()?.takeIf { it > 0 }
        val warrantyExpiryIso = warrantyExpiryIsoInput.trim().ifBlank { null }

        if (cleanName.isBlank() || odometer == null || cost == null) {
            _uiState.value = _uiState.value.copy(error = "Part name, odometer, and cost are required.", message = null)
            return false
        }

        if (warrantyExpiryIso != null) {
            runCatching { LocalDate.parse(warrantyExpiryIso) }.getOrNull() ?: run {
                _uiState.value = _uiState.value.copy(error = "Warranty expiry date must be yyyy-MM-dd.", message = null)
                return false
            }
        }

        if (editingId == null) {
            repository.addPartRecord(
                cleanName,
                cleanDate,
                odometer,
                cost,
                notes.trim(),
                costLevelOverride,
                replacementIntervalKm,
                warrantyExpiryIso,
                imageUris
            )
            _uiState.value = _uiState.value.copy(error = null, message = "Part record added.")
        } else {
            repository.updatePartRecord(
                editingId,
                cleanName,
                cleanDate,
                odometer,
                cost,
                notes.trim(),
                costLevelOverride,
                replacementIntervalKm,
                warrantyExpiryIso,
                imageUris
            )
            _uiState.value = _uiState.value.copy(error = null, message = "Part record updated.")
        }
        refresh()
        return true
    }

    fun requestDeletePart(id: String) {
        _uiState.value = _uiState.value.copy(pendingDeletePartId = id)
    }

    fun cancelDeletePart() {
        _uiState.value = _uiState.value.copy(pendingDeletePartId = null)
    }

    fun confirmDeletePart() {
        val pendingId = _uiState.value.pendingDeletePartId ?: return
        _uiState.value = _uiState.value.copy(pendingDeletePartId = null)
        repository.deletePartRecord(pendingId)
        refresh(message = "Part record deleted.")
    }

    private fun refresh(message: String? = _uiState.value.message) {
        val currentOdometerKm = repository.getCurrentOdometerKm()
        val allServices = repository.getServiceRecords()
        val allParts = repository.getPartRecords()
        val serviceTemplates = repository.getServiceTemplates()
        val serviceQuery = _uiState.value.serviceFilterQuery.trim().lowercase()
        val partQuery = _uiState.value.partFilterQuery.trim().lowercase()

        val filteredServices = if (serviceQuery.isBlank()) {
            allServices
        } else {
            allServices.filter {
                it.serviceType.lowercase().contains(serviceQuery) ||
                    it.notes.lowercase().contains(serviceQuery) ||
                    it.displayDate.lowercase().contains(serviceQuery)
            }
        }

        val filteredParts = if (partQuery.isBlank()) {
            allParts
        } else {
            allParts.filter {
                it.partName.lowercase().contains(partQuery) ||
                    it.notes.lowercase().contains(partQuery) ||
                    it.displayDate.lowercase().contains(partQuery)
            }
        }

        val lifecycleInsights = buildLifecycleInsights(allParts)
        val overduePartCount = lifecycleInsights.count { it.severity == PartLifecycleSeverity.Overdue }
        val dueSoonPartCount = lifecycleInsights.count { it.severity == PartLifecycleSeverity.DueSoon }

        _uiState.value = _uiState.value.copy(
            currentOdometerKm = currentOdometerKm,
            services = allServices,
            parts = allParts,
            filteredServices = filteredServices,
            filteredParts = filteredParts,
            lifecycleInsights = lifecycleInsights,
            overduePartCount = overduePartCount,
            dueSoonPartCount = dueSoonPartCount,
            serviceTemplates = serviceTemplates,
            error = null,
            serviceFilterQuery = _uiState.value.serviceFilterQuery,
            partFilterQuery = _uiState.value.partFilterQuery,
            pendingDeleteServiceId = _uiState.value.pendingDeleteServiceId,
            pendingDeletePartId = _uiState.value.pendingDeletePartId,
            message = message
        )
    }

    private fun buildLifecycleInsights(parts: List<PartRecord>): List<PartLifecycleInsight> {
        return parts.mapNotNull { part ->
            val warrantyDaysRemaining = part.warrantyExpiryIso
                ?.toLocalDateOrNull()
                ?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it).toInt() }

            val severity = when {
                part.remainingKmEstimate != null && part.remainingKmEstimate < 0 -> PartLifecycleSeverity.Overdue
                warrantyDaysRemaining != null && warrantyDaysRemaining < 0 -> PartLifecycleSeverity.Overdue
                part.remainingKmEstimate != null && part.remainingKmEstimate <= 500 -> PartLifecycleSeverity.DueSoon
                warrantyDaysRemaining != null && warrantyDaysRemaining <= 30 -> PartLifecycleSeverity.DueSoon
                else -> PartLifecycleSeverity.Healthy
            }

            if (severity == PartLifecycleSeverity.Healthy) return@mapNotNull null

            val detail = when {
                part.remainingKmEstimate != null && part.remainingKmEstimate < 0 -> "Overdue by ${-part.remainingKmEstimate} km"
                part.remainingKmEstimate != null && part.remainingKmEstimate <= 500 -> "Due in ${part.remainingKmEstimate} km"
                warrantyDaysRemaining != null && warrantyDaysRemaining < 0 -> "Warranty expired ${-warrantyDaysRemaining}d ago"
                warrantyDaysRemaining != null -> "Warranty expires in $warrantyDaysRemaining d"
                else -> "Needs attention"
            }

            PartLifecycleInsight(
                partId = part.id,
                title = part.partName,
                detail = detail,
                severity = severity
            )
        }.sortedWith(
            compareBy<PartLifecycleInsight> { it.severity.ordinal }
                .thenBy { it.title }
        )
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this) }.getOrNull()
}
