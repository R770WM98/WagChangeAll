package com.thisismine.myapplication.feature.maintenance.model

data class ServiceRecord(
    val id: String,
    val dateIso: String,
    val displayDate: String,
    val odometerKm: Int,
    val serviceType: String,
    val totalCostPhp: Double,
    val notes: String,
    val costLevelOverride: String? = null,
    val imageUris: List<String> = emptyList()
)

data class PartRecord(
    val id: String,
    val dateIso: String,
    val displayDate: String,
    val odometerKm: Int,
    val partName: String,
    val totalCostPhp: Double,
    val notes: String,
    val costLevelOverride: String? = null,
    val replacementIntervalKm: Int? = null,
    val warrantyExpiryIso: String? = null,
    val remainingKmEstimate: Int? = null,
    val warrantyStatusText: String? = null,
    val imageUris: List<String> = emptyList()
)
