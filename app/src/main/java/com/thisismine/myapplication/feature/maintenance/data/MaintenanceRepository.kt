package com.thisismine.myapplication.feature.maintenance.data

import com.thisismine.myapplication.feature.maintenance.model.PartRecord
import com.thisismine.myapplication.feature.maintenance.model.ServiceRecord

interface MaintenanceRepository {
    fun getCurrentOdometerKm(): Int
    fun getServiceRecords(): List<ServiceRecord>
    fun getPartRecords(): List<PartRecord>
    fun getServiceTemplates(): List<String>
    fun addServiceRecord(
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun updateServiceRecord(
        id: String,
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun deleteServiceRecord(id: String)
    fun addPartRecord(
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        replacementIntervalKm: Int? = null,
        warrantyExpiryIso: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun updatePartRecord(
        id: String,
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        replacementIntervalKm: Int? = null,
        warrantyExpiryIso: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun deletePartRecord(id: String)
}
