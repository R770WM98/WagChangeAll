package com.thisismine.myapplication.data.repository

import com.thisismine.myapplication.data.local.AppStateEntity
import com.thisismine.myapplication.data.local.FuelEntryEntity
import com.thisismine.myapplication.data.local.MaintenanceEntryEntity
import com.thisismine.myapplication.data.local.MotorcycleEntity
import com.thisismine.myapplication.data.local.OdometerReadingEntity
import com.thisismine.myapplication.data.local.PartReplacementEntity
import com.thisismine.myapplication.data.local.ServiceReminderEntity
import com.thisismine.myapplication.data.local.WagChangeDatabase
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

class RoomWagChangeRepository(
    private val database: WagChangeDatabase
) : WagChangeRepository {
    private val motorcycles = database.motorcycleDao()
    private val odometer = database.odometerDao()
    private val maintenance = database.maintenanceDao()
    private val parts = database.partDao()
    private val fuel = database.fuelDao()
    private val reminders = database.reminderDao()
    private val appState = database.appStateDao()
    private val _activeMotorcycleFlow = MutableStateFlow<MotorcycleProfile?>(null)
    override val activeMotorcycleFlow: StateFlow<MotorcycleProfile?> = _activeMotorcycleFlow.asStateFlow()

    init {
        seedIfEmpty()
        syncActiveMotorcycleFlow()
    }

    override fun getProfile(): MotorcycleProfile = requireActiveMotorcycle()

    override fun getMotorcycles(): List<MotorcycleProfile> = motorcycles.getAll().map { it.toDomain() }

    override fun getActiveMotorcycleId(): String {
        val activeId = appState.getValue(ACTIVE_MOTORCYCLE_KEY)
        if (activeId != null && motorcycles.getById(activeId) != null) return activeId

        val fallback = motorcycles.getAll().firstOrNull()?.id
            ?: throw IllegalStateException("No motorcycles found.")
        appState.upsert(AppStateEntity(key = ACTIVE_MOTORCYCLE_KEY, value = fallback))
        syncActiveMotorcycleFlow()
        return fallback
    }

    override fun setActiveMotorcycle(motorcycleId: String) {
        if (motorcycles.getById(motorcycleId) == null) return
        appState.upsert(AppStateEntity(key = ACTIVE_MOTORCYCLE_KEY, value = motorcycleId))
        syncActiveMotorcycleFlow()
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
        val motorcycleId = "bike-${UUID.randomUUID()}"
        val entity = MotorcycleEntity(
            id = motorcycleId,
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            currentOdometerKm = currentOdometerKm,
            notes = notes,
            imageUris = encodeImageUris(imageUris)
        )
        motorcycles.insert(entity)
        odometer.insert(
            OdometerReadingEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = motorcycleId,
                dateIso = LocalDate.now().toString(),
                odometerKm = currentOdometerKm
            )
        )
        setActiveMotorcycle(motorcycleId)
        syncActiveMotorcycleFlow()
        return entity.toDomain()
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
        val existing = motorcycles.getById(motorcycleId) ?: return
        motorcycles.update(
            existing.copy(
                name = name,
                plateNumber = plateNumber,
                vin = vin,
                purchaseDateIso = purchaseDateIso,
                currentOdometerKm = currentOdometerKm,
                notes = notes
            )
        )
        syncActiveMotorcycleFlow()
    }

    override fun updateMotorcycleImages(motorcycleId: String, imageUris: List<String>) {
        val existing = motorcycles.getById(motorcycleId) ?: return
        motorcycles.update(existing.copy(imageUris = encodeImageUris(imageUris)))
        syncActiveMotorcycleFlow()
    }

    override fun deleteMotorcycle(motorcycleId: String) {
        val all = motorcycles.getAll()
        if (all.size <= 1) return
        val wasActive = getActiveMotorcycleId() == motorcycleId
        val target = motorcycles.getById(motorcycleId) ?: return
        motorcycles.delete(target)

        if (wasActive) {
            val fallback = motorcycles.getAll().firstOrNull()?.id
            if (fallback != null) {
                setActiveMotorcycle(fallback)
            }
        }
        syncActiveMotorcycleFlow()
    }

    override fun getMaintenanceEntries(): List<MaintenanceEntry> {
        val activeId = getActiveMotorcycleId()
        return maintenance.getByMotorcycleId(activeId).map { it.toDomain() }
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
        maintenance.insert(
            MaintenanceEntryEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = getActiveMotorcycleId(),
                title = title,
                dateIso = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                costLevelOverride = costLevelOverride,
                imageUris = encodeImageUris(imageUris)
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
        val existing = maintenance.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        maintenance.update(
            existing.copy(
                title = title,
                dateIso = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                costLevelOverride = costLevelOverride,
                imageUris = encodeImageUris(imageUris)
            )
        )
    }

    override fun deleteMaintenanceEntry(id: String) {
        val existing = maintenance.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        maintenance.delete(existing)
    }

    override fun getPartReplacements(): List<PartReplacement> {
        val activeId = getActiveMotorcycleId()
        return parts.getByMotorcycleId(activeId).map { it.toDomain() }
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
        parts.insert(
            PartReplacementEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = getActiveMotorcycleId(),
                partName = partName,
                dateIso = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                costLevelOverride = costLevelOverride,
                replacementIntervalKm = replacementIntervalKm,
                warrantyExpiryIso = warrantyExpiryIso,
                imageUris = encodeImageUris(imageUris)
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
        val existing = parts.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        parts.update(
            existing.copy(
                partName = partName,
                dateIso = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                costLevelOverride = costLevelOverride,
                replacementIntervalKm = replacementIntervalKm,
                warrantyExpiryIso = warrantyExpiryIso,
                imageUris = encodeImageUris(imageUris)
            )
        )
    }

    override fun deletePartReplacement(id: String) {
        val existing = parts.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        parts.delete(existing)
    }

    override fun getFuelEntries(): List<FuelEntry> {
        val activeId = getActiveMotorcycleId()
        return fuel.getByMotorcycleId(activeId).map { it.toDomain() }
    }

    override fun getAllFuelEntries(): List<FuelEntry> = fuel.getAll().map { it.toDomain() }

    override fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        fuel.insert(
            FuelEntryEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = getActiveMotorcycleId(),
                dateIso = dateIso,
                liters = liters,
                costPhp = costPhp,
                distanceKm = distanceKm
            )
        )
    }

    override fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        val existing = fuel.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        fuel.update(
            existing.copy(
                dateIso = dateIso,
                liters = liters,
                costPhp = costPhp,
                distanceKm = distanceKm
            )
        )
    }

    override fun deleteFuelEntry(id: String) {
        val existing = fuel.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        fuel.delete(existing)
    }

    override fun getReminders(): List<ServiceReminder> {
        val activeId = getActiveMotorcycleId()
        return reminders.getByMotorcycleId(activeId).map { it.toDomain() }.filterNot { it.isArchived }
    }

    override fun getArchivedReminders(): List<ServiceReminder> {
        val activeId = getActiveMotorcycleId()
        return reminders.getByMotorcycleId(activeId).map { it.toDomain() }.filter { it.isArchived }
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
        reminders.insert(
            ServiceReminderEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = getActiveMotorcycleId(),
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
        val existing = reminders.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        reminders.update(
            existing.copy(
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

    override fun archiveReminder(id: String, lastCompletedAtIsoDate: String?) {
        val existing = reminders.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        reminders.update(
            existing.copy(
                isArchived = true,
                lastCompletedAtIsoDate = lastCompletedAtIsoDate,
                completionCount = existing.completionCount + 1
            )
        )
    }

    override fun deleteReminder(id: String) {
        val existing = reminders.getById(id) ?: return
        if (existing.motorcycleId != getActiveMotorcycleId()) return
        reminders.delete(existing)
    }

    override fun getOdometerReadings(): List<OdometerReading> {
        val activeId = getActiveMotorcycleId()
        return odometer.getByMotorcycleId(activeId).map { it.toDomain() }
    }

    override fun addOdometerReading(odometerKm: Int, dateIso: String) {
        val activeId = getActiveMotorcycleId()
        odometer.insert(
            OdometerReadingEntity(
                id = UUID.randomUUID().toString(),
                motorcycleId = activeId,
                dateIso = dateIso,
                odometerKm = odometerKm
            )
        )
        motorcycles.updateCurrentOdometer(activeId, odometerKm)
        syncActiveMotorcycleFlow()
    }

    override fun exportBackupSnapshot(): AppBackupSnapshot {
        return AppBackupSnapshot(
            activeMotorcycleId = getActiveMotorcycleId(),
            motorcycles = motorcycles.getAll().map { it.toDomain() },
            odometerReadings = odometer.getAll().map { it.toDomain() },
            maintenanceEntries = maintenance.getAll().map { it.toDomain() },
            partReplacements = parts.getAll().map { it.toDomain() },
            fuelEntries = fuel.getAll().map { it.toDomain() },
            reminders = reminders.getAll().map { it.toDomain() }
        )
    }

    override fun importBackupSnapshot(snapshot: AppBackupSnapshot) {
        if (snapshot.motorcycles.isEmpty()) return

        database.runInTransaction {
            appState.clearAll()
            reminders.clearAll()
            fuel.clearAll()
            parts.clearAll()
            maintenance.clearAll()
            odometer.clearAll()
            motorcycles.clearAll()

            motorcycles.insertAll(snapshot.motorcycles.map { it.toEntity() })
            odometer.insertAll(snapshot.odometerReadings.map { it.toEntity() })
            maintenance.insertAll(snapshot.maintenanceEntries.map { it.toEntity() })
            parts.insertAll(snapshot.partReplacements.map { it.toEntity() })
            fuel.insertAll(snapshot.fuelEntries.map { it.toEntity() })
            reminders.insertAll(snapshot.reminders.map { it.toEntity() })

            val restoredActiveId = snapshot.activeMotorcycleId.takeIf { id ->
                snapshot.motorcycles.any { it.id == id }
            } ?: snapshot.motorcycles.first().id
            appState.upsert(AppStateEntity(key = ACTIVE_MOTORCYCLE_KEY, value = restoredActiveId))
        }

        syncActiveMotorcycleFlow()
    }

    override fun exportMaintenanceCsv(): String {
        val motorcycleNames = motorcycles.getAll().associate { it.id to it.name }
        val header = "motorcycleId,motorcycleName,entryId,title,dateIso,odometerKm,totalCostPhp,notes,costLevelOverride"
        val rows = maintenance.getAll()
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
        val motorcycleNames = motorcycles.getAll().associate { it.id to it.name }
        val header = "motorcycleId,motorcycleName,entryId,dateIso,liters,costPhp,distanceKm"
        val rows = fuel.getAll()
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
        val activeId = getActiveMotorcycleId()
        return motorcycles.getById(activeId)?.toDomain()
            ?: throw IllegalStateException("Active motorcycle not found.")
    }

    private fun seedIfEmpty() {
        if (motorcycles.getAll().isNotEmpty()) return

        val starter = MotorcycleEntity(
            id = "bike-1",
            name = "Daily Ride",
            plateNumber = "ABC-1234",
            vin = "JYARN23E1PA123456",
            purchaseDateIso = "2025-05-10",
            currentOdometerKm = 15420,
            notes = "Main city bike",
            imageUris = ""
        )
        motorcycles.insert(starter)

        odometer.insert(
            OdometerReadingEntity(
                id = "od-1",
                motorcycleId = starter.id,
                dateIso = "2026-03-20",
                odometerKm = starter.currentOdometerKm
            )
        )

        maintenance.insert(
            MaintenanceEntryEntity(
                id = "m1",
                motorcycleId = starter.id,
                title = "Engine Oil Change",
                dateIso = "2026-03-10",
                odometerKm = 15000,
                totalCostPhp = 780.0,
                notes = "Changed oil and cleaned air filter",
                costLevelOverride = null
            )
        )

        parts.insert(
            PartReplacementEntity(
                id = "p1",
                motorcycleId = starter.id,
                partName = "Brake Pads",
                dateIso = "2026-03-12",
                odometerKm = 15100,
                totalCostPhp = 450.0,
                notes = "Front pads replaced",
                costLevelOverride = null
            )
        )

        fuel.insert(
            FuelEntryEntity(
                id = "f1",
                motorcycleId = starter.id,
                dateIso = "2026-03-15",
                liters = 4.8,
                costPhp = 320.0,
                distanceKm = 165.0
            )
        )

        reminders.insert(
            ServiceReminderEntity(
                id = "r1",
                motorcycleId = starter.id,
                title = "CVT Cleaning",
                triggerAtKm = 16000,
                triggerAtIsoDate = null,
                repeatingIntervalKm = null,
                lastTriggeredAtKm = null
            )
        )

        setActiveMotorcycle(starter.id)
    }

    private fun MotorcycleEntity.toDomain(): MotorcycleProfile = MotorcycleProfile(
        id = id,
        name = name,
        plateNumber = plateNumber,
        vin = vin,
        purchaseDateIso = purchaseDateIso,
        currentOdometerKm = currentOdometerKm,
        notes = notes,
        imageUris = decodeImageUris(imageUris)
    )

    private fun OdometerReadingEntity.toDomain(): OdometerReading = OdometerReading(
        id = id,
        motorcycleId = motorcycleId,
        dateIso = dateIso,
        odometerKm = odometerKm
    )

    private fun MaintenanceEntryEntity.toDomain(): MaintenanceEntry = MaintenanceEntry(
        id = id,
        motorcycleId = motorcycleId,
        title = title,
        dateIso = dateIso,
        odometerKm = odometerKm,
        totalCostPhp = totalCostPhp,
        notes = notes,
        costLevelOverride = costLevelOverride,
        imageUris = decodeImageUris(imageUris)
    )

    private fun PartReplacementEntity.toDomain(): PartReplacement = PartReplacement(
        id = id,
        motorcycleId = motorcycleId,
        partName = partName,
        dateIso = dateIso,
        odometerKm = odometerKm,
        totalCostPhp = totalCostPhp,
        notes = notes,
        costLevelOverride = costLevelOverride,
        replacementIntervalKm = replacementIntervalKm,
        warrantyExpiryIso = warrantyExpiryIso,
        imageUris = decodeImageUris(imageUris)
    )

    private fun FuelEntryEntity.toDomain(): FuelEntry = FuelEntry(
        id = id,
        motorcycleId = motorcycleId,
        dateIso = dateIso,
        liters = liters,
        costPhp = costPhp,
        distanceKm = distanceKm
    )

    private fun ServiceReminderEntity.toDomain(): ServiceReminder = ServiceReminder(
        id = id,
        motorcycleId = motorcycleId,
        title = title,
        triggerAtKm = triggerAtKm,
        triggerAtIsoDate = triggerAtIsoDate,
        repeatingIntervalKm = repeatingIntervalKm,
        lastTriggeredAtKm = lastTriggeredAtKm,
        completionCount = completionCount,
        lastCompletedAtIsoDate = lastCompletedAtIsoDate,
        isArchived = isArchived
    )

    private fun MotorcycleProfile.toEntity(): MotorcycleEntity = MotorcycleEntity(
        id = id,
        name = name,
        plateNumber = plateNumber,
        vin = vin,
        purchaseDateIso = purchaseDateIso,
        currentOdometerKm = currentOdometerKm,
        notes = notes,
        imageUris = encodeImageUris(imageUris)
    )

    private fun OdometerReading.toEntity(): OdometerReadingEntity = OdometerReadingEntity(
        id = id,
        motorcycleId = motorcycleId,
        dateIso = dateIso,
        odometerKm = odometerKm
    )

    private fun MaintenanceEntry.toEntity(): MaintenanceEntryEntity = MaintenanceEntryEntity(
        id = id,
        motorcycleId = motorcycleId,
        title = title,
        dateIso = dateIso,
        odometerKm = odometerKm,
        totalCostPhp = totalCostPhp,
        notes = notes,
        costLevelOverride = costLevelOverride,
        imageUris = encodeImageUris(imageUris)
    )

    private fun PartReplacement.toEntity(): PartReplacementEntity = PartReplacementEntity(
        id = id,
        motorcycleId = motorcycleId,
        partName = partName,
        dateIso = dateIso,
        odometerKm = odometerKm,
        totalCostPhp = totalCostPhp,
        notes = notes,
        costLevelOverride = costLevelOverride,
        replacementIntervalKm = replacementIntervalKm,
        warrantyExpiryIso = warrantyExpiryIso,
        imageUris = encodeImageUris(imageUris)
    )

    private fun FuelEntry.toEntity(): FuelEntryEntity = FuelEntryEntity(
        id = id,
        motorcycleId = motorcycleId,
        dateIso = dateIso,
        liters = liters,
        costPhp = costPhp,
        distanceKm = distanceKm
    )

    private fun ServiceReminder.toEntity(): ServiceReminderEntity = ServiceReminderEntity(
        id = id,
        motorcycleId = motorcycleId,
        title = title,
        triggerAtKm = triggerAtKm,
        triggerAtIsoDate = triggerAtIsoDate,
        repeatingIntervalKm = repeatingIntervalKm,
        lastTriggeredAtKm = lastTriggeredAtKm,
        completionCount = completionCount,
        lastCompletedAtIsoDate = lastCompletedAtIsoDate,
        isArchived = isArchived
    )

    private fun encodeImageUris(values: List<String>): String =
        values.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(separator = "\n")

    private fun decodeImageUris(raw: String): List<String> =
        raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun syncActiveMotorcycleFlow() {
        _activeMotorcycleFlow.value = motorcycles.getById(getActiveMotorcycleId())?.toDomain()
    }

    companion object {
        private const val ACTIVE_MOTORCYCLE_KEY = "active_motorcycle_id"
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
