package com.thisismine.myapplication.data.repository

import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.domain.model.PartReplacement
import com.thisismine.myapplication.domain.model.ServiceReminder
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface WagChangeRepository {
    val activeMotorcycleFlow: StateFlow<MotorcycleProfile?>

    fun getProfile(): MotorcycleProfile
    fun getMotorcycles(): List<MotorcycleProfile>
    fun getActiveMotorcycleId(): String
    fun setActiveMotorcycle(motorcycleId: String)
    fun addMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String,
        imageUris: List<String> = emptyList()
    ): MotorcycleProfile
    fun updateMotorcycle(
        motorcycleId: String,
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String
    )
    fun updateMotorcycleImages(motorcycleId: String, imageUris: List<String>)
    fun deleteMotorcycle(motorcycleId: String)
    fun getMaintenanceEntries(): List<MaintenanceEntry>
    fun addMaintenanceEntry(
        title: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun updateMaintenanceEntry(
        id: String,
        title: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String? = null,
        imageUris: List<String> = emptyList()
    )
    fun deleteMaintenanceEntry(id: String)
    fun getPartReplacements(): List<PartReplacement>
    fun addPartReplacement(
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
    fun updatePartReplacement(
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
    fun deletePartReplacement(id: String)
    fun getFuelEntries(): List<FuelEntry>
    fun getAllFuelEntries(): List<FuelEntry>
    fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double)
    fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double)
    fun deleteFuelEntry(id: String)
    fun getReminders(): List<ServiceReminder>
    fun getArchivedReminders(): List<ServiceReminder>
    fun addReminder(
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int = 0,
        lastCompletedAtIsoDate: String? = null,
        isArchived: Boolean = false
    )
    fun updateReminder(
        id: String,
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int = 0,
        lastCompletedAtIsoDate: String? = null,
        isArchived: Boolean = false
    )
    fun archiveReminder(id: String, lastCompletedAtIsoDate: String? = null)
    fun deleteReminder(id: String)
    fun getOdometerReadings(): List<OdometerReading>
    fun addOdometerReading(odometerKm: Int, dateIso: String)

    fun exportBackupSnapshot(): AppBackupSnapshot
    fun importBackupSnapshot(snapshot: AppBackupSnapshot)
    fun exportMaintenanceCsv(): String
    fun exportFuelCsv(): String
}

class InMemoryWagChangeRepository : WagChangeRepository {
    private val motorcycles = mutableListOf(
        MotorcycleProfile(
            id = "bike-1",
            name = "Daily Ride",
            plateNumber = "ABC-1234",
            vin = "JYARN23E1PA123456",
            purchaseDateIso = "2025-05-10",
            currentOdometerKm = 15_420,
            notes = "Main city bike",
            imageUris = emptyList()
        )
    )

    private val _activeMotorcycleFlow = MutableStateFlow<MotorcycleProfile?>(motorcycles.firstOrNull())
    override val activeMotorcycleFlow: StateFlow<MotorcycleProfile?> = _activeMotorcycleFlow.asStateFlow()

    private var activeMotorcycleId: String = motorcycles.first().id

    private val maintenanceEntries = mutableListOf(
        MaintenanceEntry(
            id = "m1",
            motorcycleId = "bike-1",
            title = "Engine Oil Change",
            dateIso = "2026-03-10",
            odometerKm = 15_000,
            totalCostPhp = 780.0,
            notes = "Changed oil and cleaned air filter"
        )
    )

    private val partReplacements = mutableListOf(
        PartReplacement(
            id = "p1",
            motorcycleId = "bike-1",
            partName = "Brake Pads",
            dateIso = "2026-03-12",
            odometerKm = 15_100,
            totalCostPhp = 450.0,
            notes = "Front pads replaced",
            replacementIntervalKm = 6000,
            warrantyExpiryIso = "2026-12-31"
        )
    )

    private val fuelEntries = mutableListOf(
        FuelEntry(
            id = "f1",
            motorcycleId = "bike-1",
            dateIso = "2026-03-15",
            liters = 4.8,
            costPhp = 320.0,
            distanceKm = 165.0
        )
    )

    private val reminders = mutableListOf(
        ServiceReminder(
            id = "r1",
            motorcycleId = "bike-1",
            title = "CVT Cleaning",
            triggerAtKm = 16_000,
            triggerAtIsoDate = null,
            repeatingIntervalKm = null,
            lastTriggeredAtKm = null,
            completionCount = 0,
            lastCompletedAtIsoDate = null,
            isArchived = false
        )
    )

    private val odometerReadings = mutableListOf(
        OdometerReading(
            id = "od-1",
            motorcycleId = "bike-1",
            dateIso = "2026-03-20",
            odometerKm = motorcycles.first().currentOdometerKm
        )
    )

    override fun getProfile(): MotorcycleProfile = requireActiveMotorcycle()

    override fun getMotorcycles(): List<MotorcycleProfile> = motorcycles.toList()

    override fun getActiveMotorcycleId(): String = activeMotorcycleId

    override fun setActiveMotorcycle(motorcycleId: String) {
        if (motorcycles.any { it.id == motorcycleId }) {
            activeMotorcycleId = motorcycleId
            syncActiveMotorcycleFlow()
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
    ): MotorcycleProfile {
        val newMotorcycle = MotorcycleProfile(
            id = "bike-${UUID.randomUUID()}",
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            currentOdometerKm = currentOdometerKm,
            notes = notes,
            imageUris = imageUris
        )
        motorcycles.add(newMotorcycle)
        odometerReadings.add(
            OdometerReading(
                id = UUID.randomUUID().toString(),
                motorcycleId = newMotorcycle.id,
                dateIso = LocalDate.now().toString(),
                odometerKm = currentOdometerKm
            )
        )
        activeMotorcycleId = newMotorcycle.id
        syncActiveMotorcycleFlow()
        return newMotorcycle
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
        val index = motorcycles.indexOfFirst { it.id == motorcycleId }
        if (index < 0) return
        val existing = motorcycles[index]
        motorcycles[index] = existing.copy(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            currentOdometerKm = currentOdometerKm,
            notes = notes
        )
        syncActiveMotorcycleFlow()
    }

    override fun updateMotorcycleImages(motorcycleId: String, imageUris: List<String>) {
        val index = motorcycles.indexOfFirst { it.id == motorcycleId }
        if (index < 0) return
        motorcycles[index] = motorcycles[index].copy(imageUris = imageUris)
        syncActiveMotorcycleFlow()
    }

    override fun deleteMotorcycle(motorcycleId: String) {
        if (motorcycles.size <= 1) return
        motorcycles.removeAll { it.id == motorcycleId }
        odometerReadings.removeAll { it.motorcycleId == motorcycleId }
        maintenanceEntries.removeAll { it.motorcycleId == motorcycleId }
        partReplacements.removeAll { it.motorcycleId == motorcycleId }
        fuelEntries.removeAll { it.motorcycleId == motorcycleId }
        reminders.removeAll { it.motorcycleId == motorcycleId }

        if (activeMotorcycleId == motorcycleId) {
            activeMotorcycleId = motorcycles.first().id
        }
        syncActiveMotorcycleFlow()
    }

    override fun getMaintenanceEntries(): List<MaintenanceEntry> {
        val activeId = activeMotorcycleId
        return maintenanceEntries.filter { it.motorcycleId == activeId }
    }

    override fun addMaintenanceEntry(
        title: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        maintenanceEntries.add(
            MaintenanceEntry(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeMotorcycleId,
                title = title,
                dateIso = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                costLevelOverride = costLevelOverride,
                imageUris = imageUris
            )
        )
    }

    override fun updateMaintenanceEntry(
        id: String,
        title: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        val idx = maintenanceEntries.indexOfFirst { it.id == id && it.motorcycleId == activeMotorcycleId }
        if (idx < 0) return
        val existing = maintenanceEntries[idx]
        maintenanceEntries[idx] = existing.copy(
            title = title,
            dateIso = dateIso,
            odometerKm = odometerKm,
            totalCostPhp = totalCostPhp,
            notes = notes,
            costLevelOverride = costLevelOverride,
            imageUris = imageUris
        )
    }

    override fun deleteMaintenanceEntry(id: String) {
        maintenanceEntries.removeAll { it.id == id && it.motorcycleId == activeMotorcycleId }
    }

    override fun getPartReplacements(): List<PartReplacement> {
        val activeId = activeMotorcycleId
        return partReplacements.filter { it.motorcycleId == activeId }
    }

    override fun addPartReplacement(
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
        partReplacements.add(
            PartReplacement(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeMotorcycleId,
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
        )
    }

    override fun updatePartReplacement(
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
        val idx = partReplacements.indexOfFirst { it.id == id && it.motorcycleId == activeMotorcycleId }
        if (idx < 0) return
        val existing = partReplacements[idx]
        partReplacements[idx] = existing.copy(
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

    override fun deletePartReplacement(id: String) {
        partReplacements.removeAll { it.id == id && it.motorcycleId == activeMotorcycleId }
    }

    override fun getFuelEntries(): List<FuelEntry> {
        val activeId = activeMotorcycleId
        return fuelEntries.filter { it.motorcycleId == activeId }
    }

    override fun getAllFuelEntries(): List<FuelEntry> = fuelEntries.toList()

    override fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        fuelEntries.add(
            FuelEntry(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeMotorcycleId,
                dateIso = dateIso,
                liters = liters,
                costPhp = costPhp,
                distanceKm = distanceKm
            )
        )
    }

    override fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        val idx = fuelEntries.indexOfFirst { it.id == id && it.motorcycleId == activeMotorcycleId }
        if (idx < 0) return
        fuelEntries[idx] = fuelEntries[idx].copy(
            dateIso = dateIso,
            liters = liters,
            costPhp = costPhp,
            distanceKm = distanceKm
        )
    }

    override fun deleteFuelEntry(id: String) {
        fuelEntries.removeAll { it.id == id && it.motorcycleId == activeMotorcycleId }
    }

    override fun getReminders(): List<ServiceReminder> {
        val activeId = activeMotorcycleId
        return reminders.filter { it.motorcycleId == activeId && !it.isArchived }
    }

    override fun getArchivedReminders(): List<ServiceReminder> {
        val activeId = activeMotorcycleId
        return reminders.filter { it.motorcycleId == activeId && it.isArchived }
    }

    override fun addReminder(
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int,
        lastCompletedAtIsoDate: String?,
        isArchived: Boolean
    ) {
        reminders.add(
            ServiceReminder(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeMotorcycleId,
                title = title,
                triggerAtKm = triggerAtKm,
                triggerAtIsoDate = triggerAtIsoDate,
                repeatingIntervalKm = repeatingIntervalKm,
                lastTriggeredAtKm = lastTriggeredAtKm,
                completionCount = completionCount,
                lastCompletedAtIsoDate = lastCompletedAtIsoDate,
                isArchived = isArchived
            )
        )
    }

    override fun updateReminder(
        id: String,
        title: String,
        triggerAtKm: Int,
        triggerAtIsoDate: String?,
        repeatingIntervalKm: Int?,
        lastTriggeredAtKm: Int?,
        completionCount: Int,
        lastCompletedAtIsoDate: String?,
        isArchived: Boolean
    ) {
        val idx = reminders.indexOfFirst { it.id == id && it.motorcycleId == activeMotorcycleId }
        if (idx < 0) return
        reminders[idx] = reminders[idx].copy(
            title = title,
            triggerAtKm = triggerAtKm,
            triggerAtIsoDate = triggerAtIsoDate,
            repeatingIntervalKm = repeatingIntervalKm,
            lastTriggeredAtKm = lastTriggeredAtKm,
            completionCount = completionCount,
            lastCompletedAtIsoDate = lastCompletedAtIsoDate,
            isArchived = isArchived
        )
    }

    override fun archiveReminder(id: String, lastCompletedAtIsoDate: String?) {
        val idx = reminders.indexOfFirst { it.id == id && it.motorcycleId == activeMotorcycleId }
        if (idx < 0) return
        reminders[idx] = reminders[idx].copy(
            isArchived = true,
            lastCompletedAtIsoDate = lastCompletedAtIsoDate,
            completionCount = reminders[idx].completionCount + 1
        )
    }

    override fun deleteReminder(id: String) {
        reminders.removeAll { it.id == id && it.motorcycleId == activeMotorcycleId }
    }

    override fun getOdometerReadings(): List<OdometerReading> {
        val activeId = activeMotorcycleId
        return odometerReadings.filter { it.motorcycleId == activeId }
    }

    override fun addOdometerReading(odometerKm: Int, dateIso: String) {
        val activeMotorcycle = requireActiveMotorcycle()
        odometerReadings.add(
            OdometerReading(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeMotorcycle.id,
                dateIso = dateIso,
                odometerKm = odometerKm
            )
        )
        val activeIndex = motorcycles.indexOfFirst { it.id == activeMotorcycle.id }
        if (activeIndex >= 0) {
            motorcycles[activeIndex] = motorcycles[activeIndex].copy(currentOdometerKm = odometerKm)
        }
        syncActiveMotorcycleFlow()
    }

    override fun exportBackupSnapshot(): AppBackupSnapshot {
        return AppBackupSnapshot(
            activeMotorcycleId = activeMotorcycleId,
            motorcycles = motorcycles.toList(),
            odometerReadings = odometerReadings.toList(),
            maintenanceEntries = maintenanceEntries.toList(),
            partReplacements = partReplacements.toList(),
            fuelEntries = fuelEntries.toList(),
            reminders = reminders.toList()
        )
    }

    override fun importBackupSnapshot(snapshot: AppBackupSnapshot) {
        if (snapshot.motorcycles.isEmpty()) return
        motorcycles.clear()
        motorcycles.addAll(snapshot.motorcycles)
        odometerReadings.clear()
        odometerReadings.addAll(snapshot.odometerReadings)
        maintenanceEntries.clear()
        maintenanceEntries.addAll(snapshot.maintenanceEntries)
        partReplacements.clear()
        partReplacements.addAll(snapshot.partReplacements)
        fuelEntries.clear()
        fuelEntries.addAll(snapshot.fuelEntries)
        reminders.clear()
        reminders.addAll(snapshot.reminders)

        activeMotorcycleId = snapshot.activeMotorcycleId.takeIf { id ->
            motorcycles.any { it.id == id }
        } ?: motorcycles.first().id
        syncActiveMotorcycleFlow()
    }

    override fun exportMaintenanceCsv(): String {
        val motorcycleNames = motorcycles.associate { it.id to it.name }
        val header = "motorcycleId,motorcycleName,entryId,title,dateIso,odometerKm,totalCostPhp,notes,costLevelOverride"
        val rows = maintenanceEntries
            .sortedBy { it.dateIso }
            .map { entry ->
            listOf(
                entry.motorcycleId,
                motorcycleNames[entry.motorcycleId].orEmpty(),
                entry.id,
                entry.title,
                entry.dateIso,
                entry.odometerKm.toString(),
                String.format(Locale.US, "%.2f", entry.totalCostPhp),
                entry.notes,
                entry.costLevelOverride.orEmpty()
            ).joinToString(",") { it.toCsvCell() }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    override fun exportFuelCsv(): String {
        val motorcycleNames = motorcycles.associate { it.id to it.name }
        val header = "motorcycleId,motorcycleName,entryId,dateIso,liters,costPhp,distanceKm"
        val rows = fuelEntries
            .sortedBy { it.dateIso }
            .map { entry ->
            listOf(
                entry.motorcycleId,
                motorcycleNames[entry.motorcycleId].orEmpty(),
                entry.id,
                entry.dateIso,
                String.format(Locale.US, "%.2f", entry.liters),
                String.format(Locale.US, "%.2f", entry.costPhp),
                String.format(Locale.US, "%.2f", entry.distanceKm)
            ).joinToString(",") { it.toCsvCell() }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun requireActiveMotorcycle(): MotorcycleProfile {
        return motorcycles.firstOrNull { it.id == activeMotorcycleId } ?: motorcycles.first()
    }

    private fun syncActiveMotorcycleFlow() {
        _activeMotorcycleFlow.value = motorcycles.firstOrNull { it.id == activeMotorcycleId }
    }
}

private fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n')) {
        "\"$escaped\""
    } else {
        escaped
    }
}
