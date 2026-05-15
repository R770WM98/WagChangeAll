package com.thisismine.myapplication.feature.maintenance.data

import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.maintenance.model.PartRecord
import com.thisismine.myapplication.feature.maintenance.model.ServiceRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class WagChangeMaintenanceRepository(
    private val wagChangeRepository: WagChangeRepository
) : MaintenanceRepository {
    private val sourceDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val uiDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

    override fun getCurrentOdometerKm(): Int = wagChangeRepository.getProfile().currentOdometerKm

    override fun getServiceTemplates(): List<String> = listOf(
        "Engine Oil Change",
        "Brake Fluid Flush",
        "CVT Cleaning",
        "Tire Rotation / Check"
    )

    override fun getServiceRecords(): List<ServiceRecord> {
        return wagChangeRepository.getMaintenanceEntries().map { entry ->
            ServiceRecord(
                id = entry.id,
                dateIso = entry.dateIso,
                displayDate = normalizeDate(entry.dateIso),
                odometerKm = entry.odometerKm,
                serviceType = entry.title,
                totalCostPhp = entry.totalCostPhp,
                notes = entry.notes,
                costLevelOverride = entry.costLevelOverride,
                imageUris = entry.imageUris
            )
        }
    }

    override fun getPartRecords(): List<PartRecord> {
        val currentOdometer = getCurrentOdometerKm()
        val today = LocalDate.now()
        return wagChangeRepository.getPartReplacements().map { entry ->
            val remainingKm = entry.replacementIntervalKm?.let { interval ->
                (entry.odometerKm + interval) - currentOdometer
            }
            val warrantyStatus = entry.warrantyExpiryIso?.let { expiryRaw ->
                expiryRaw.toLocalDateOrNull()?.let { expiryDate ->
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate).toInt()
                    when {
                        days < 0 -> "Warranty expired ${-days}d ago"
                        days == 0 -> "Warranty expires today"
                        else -> "Warranty $days d left"
                    }
                }
            }

            PartRecord(
                id = entry.id,
                dateIso = entry.dateIso,
                displayDate = normalizeDate(entry.dateIso),
                odometerKm = entry.odometerKm,
                partName = entry.partName,
                totalCostPhp = entry.totalCostPhp,
                notes = entry.notes,
                costLevelOverride = entry.costLevelOverride,
                replacementIntervalKm = entry.replacementIntervalKm,
                warrantyExpiryIso = entry.warrantyExpiryIso,
                remainingKmEstimate = remainingKm,
                warrantyStatusText = warrantyStatus,
                imageUris = entry.imageUris
            )
        }
    }

    override fun addServiceRecord(
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        wagChangeRepository.addMaintenanceEntry(
            title = serviceType,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = totalCostPhp,
            notes = notes,
            costLevelOverride = costLevelOverride,
            imageUris = imageUris
        )
    }

    override fun updateServiceRecord(
        id: String,
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        wagChangeRepository.updateMaintenanceEntry(
            id = id,
            title = serviceType,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = totalCostPhp,
            notes = notes,
            costLevelOverride = costLevelOverride,
            imageUris = imageUris
        )
    }

    override fun deleteServiceRecord(id: String) {
        wagChangeRepository.deleteMaintenanceEntry(id)
    }

    override fun addPartRecord(
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        replacementIntervalKm: Int?,
        warrantyExpiryIso: String?,
        imageUris: List<String>
    ) {
        wagChangeRepository.addPartReplacement(
            partName = partName,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = totalCostPhp,
            notes = notes,
            costLevelOverride = costLevelOverride,
            replacementIntervalKm = replacementIntervalKm,
            warrantyExpiryIso = warrantyExpiryIso,
            imageUris = imageUris
        )
    }

    override fun updatePartRecord(
        id: String,
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        replacementIntervalKm: Int?,
        warrantyExpiryIso: String?,
        imageUris: List<String>
    ) {
        wagChangeRepository.updatePartReplacement(
            id = id,
            partName = partName,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = totalCostPhp,
            notes = notes,
            costLevelOverride = costLevelOverride,
            replacementIntervalKm = replacementIntervalKm,
            warrantyExpiryIso = warrantyExpiryIso,
            imageUris = imageUris
        )
    }

    override fun deletePartRecord(id: String) {
        wagChangeRepository.deletePartReplacement(id)
    }

    private fun normalizeDate(rawDate: String): String {
        return try {
            LocalDate.parse(rawDate, sourceDateFormatter).format(uiDateFormatter)
        } catch (_: DateTimeParseException) {
            rawDate
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        return try {
            LocalDate.parse(this, sourceDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
