package com.thisismine.myapplication.domain.model

data class MotorcycleProfile(
    val id: String,
    val name: String,
    val plateNumber: String,
    val vin: String,
    val purchaseDateIso: String,
    val currentOdometerKm: Int,
    val notes: String,
    val imageUris: List<String> = emptyList()
) {
    @Suppress("unused")
    val primaryImageUri: String?
        get() = imageUris.firstOrNull()
}

data class OdometerReading(
    val id: String,
    val motorcycleId: String,
    val dateIso: String,
    val odometerKm: Int
)

data class MaintenanceEntry(
    val id: String,
    val motorcycleId: String,
    val title: String,
    val dateIso: String,
    val odometerKm: Int,
    val totalCostPhp: Double,
    val notes: String,
    val costLevelOverride: String? = null,
    val imageUris: List<String> = emptyList()
)

data class PartReplacement(
    val id: String,
    val motorcycleId: String,
    val partName: String,
    val dateIso: String,
    val odometerKm: Int,
    val totalCostPhp: Double,
    val notes: String,
    val costLevelOverride: String? = null,
    val replacementIntervalKm: Int? = null,
    val warrantyExpiryIso: String? = null,
    val imageUris: List<String> = emptyList()
)

data class FuelEntry(
    val id: String,
    val motorcycleId: String,
    val dateIso: String,
    val liters: Double,
    val costPhp: Double,
    val distanceKm: Double
)

data class ServiceReminder(
    val id: String,
    val motorcycleId: String,
    val title: String,
    val triggerAtKm: Int,
    val triggerAtIsoDate: String?,
    val repeatingIntervalKm: Int? = null,
    val lastTriggeredAtKm: Int? = null,
    val completionCount: Int = 0,
    val lastCompletedAtIsoDate: String? = null,
    val isArchived: Boolean = false
) {
    val isRepeating: Boolean
        get() = repeatingIntervalKm != null && repeatingIntervalKm > 0
}
