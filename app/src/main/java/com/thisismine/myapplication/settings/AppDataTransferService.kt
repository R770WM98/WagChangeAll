package com.thisismine.myapplication.settings

import com.thisismine.myapplication.data.repository.AppBackupSnapshot
import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.domain.model.PartReplacement
import com.thisismine.myapplication.domain.model.ServiceReminder
import com.thisismine.myapplication.core.ui.CardDensityMode
import com.thisismine.myapplication.ui.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject

class AppDataTransferService {
    fun toBackupJson(snapshot: AppBackupSnapshot, settings: AppSettings? = null): String {
        return JSONObject().apply {
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("snapshot", JSONObject().apply {
                put("schemaVersion", snapshot.schemaVersion)
                put("activeMotorcycleId", snapshot.activeMotorcycleId)
                put("motorcycles", JSONArray().apply {
                    snapshot.motorcycles.forEach { addMotorcycle(it) }
                })
                put("odometerReadings", JSONArray().apply {
                    snapshot.odometerReadings.forEach { addOdometerReading(it) }
                })
                put("maintenanceEntries", JSONArray().apply {
                    snapshot.maintenanceEntries.forEach { addMaintenanceEntry(it) }
                })
                put("partReplacements", JSONArray().apply {
                    snapshot.partReplacements.forEach { addPartReplacement(it) }
                })
                put("fuelEntries", JSONArray().apply {
                    snapshot.fuelEntries.forEach { addFuelEntry(it) }
                })
                put("reminders", JSONArray().apply {
                    snapshot.reminders.forEach { addReminder(it) }
                })
            })
            if (settings != null) {
                put("settings", JSONObject().apply {
                    put("themeMode", settings.themeMode.name)
                    put("densityMode", settings.densityMode.name)
                    put("showActiveBikeBadge", settings.showActiveBikeBadge)
                    put("reduceMotion", settings.reduceMotion)
                    put("geminiEnabled", settings.geminiEnabled)
                    put("redactSensitiveDataForAi", settings.redactSensitiveDataForAi)
                })
            }
        }.toString(2)
    }

    fun fromBackupJson(rawJson: String): AppBackupBundle {
        val root = JSONObject(rawJson)
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion !in SUPPORTED_BACKUP_VERSIONS) {
            throw IllegalArgumentException("Unsupported backup version: $schemaVersion")
        }

        val snapshotRoot = root.optJSONObject("snapshot") ?: root
        val motorcycles = snapshotRoot.optJSONArray("motorcycles").toMotorcycles()
        if (motorcycles.isEmpty()) {
            throw IllegalArgumentException("Backup has no motorcycles.")
        }

        val snapshot = AppBackupSnapshot(
            schemaVersion = snapshotRoot.optInt("schemaVersion", AppBackupSnapshot.SCHEMA_VERSION),
            activeMotorcycleId = snapshotRoot.optString("activeMotorcycleId"),
            motorcycles = motorcycles,
            odometerReadings = snapshotRoot.optJSONArray("odometerReadings").toOdometerReadings(),
            maintenanceEntries = snapshotRoot.optJSONArray("maintenanceEntries").toMaintenanceEntries(),
            partReplacements = snapshotRoot.optJSONArray("partReplacements").toPartReplacements(),
            fuelEntries = snapshotRoot.optJSONArray("fuelEntries").toFuelEntries(),
            reminders = snapshotRoot.optJSONArray("reminders").toReminders()
        )

        return AppBackupBundle(
            snapshot = snapshot.validateAndNormalize(),
            settings = root.optJSONObject("settings").toAppSettingsOrNull()
        )
    }
}

private const val BACKUP_SCHEMA_VERSION = 3
private val SUPPORTED_BACKUP_VERSIONS = setOf(2, BACKUP_SCHEMA_VERSION)

private fun AppBackupSnapshot.validateAndNormalize(): AppBackupSnapshot {
    validateUniqueIds(motorcycles.map { it.id }, "motorcycles")
    validateUniqueIds(odometerReadings.map { it.id }, "odometer readings")
    validateUniqueIds(maintenanceEntries.map { it.id }, "maintenance entries")
    validateUniqueIds(partReplacements.map { it.id }, "part replacements")
    validateUniqueIds(fuelEntries.map { it.id }, "fuel entries")
    validateUniqueIds(reminders.map { it.id }, "reminders")

    val motorcycleIds = motorcycles.map { it.id }.toSet()
    require(motorcycleIds.all { it.isNotBlank() }) { "Backup contains a motorcycle with a blank id." }

    require(motorcycles.all { it.name.isNotBlank() }) { "Backup contains a motorcycle with a blank name." }

    requireReferencedMotorcycles("odometer readings", odometerReadings.map { it.motorcycleId }, motorcycleIds)
    requireReferencedMotorcycles("maintenance entries", maintenanceEntries.map { it.motorcycleId }, motorcycleIds)
    requireReferencedMotorcycles("part replacements", partReplacements.map { it.motorcycleId }, motorcycleIds)
    requireReferencedMotorcycles("fuel entries", fuelEntries.map { it.motorcycleId }, motorcycleIds)
    requireReferencedMotorcycles("reminders", reminders.map { it.motorcycleId }, motorcycleIds)

    val normalizedActiveId = activeMotorcycleId.takeIf { it.isNotBlank() && it in motorcycleIds }
        ?: motorcycles.first().id

    return copy(activeMotorcycleId = normalizedActiveId)
}

private fun validateUniqueIds(ids: List<String>, label: String) {
    require(ids.all { it.isNotBlank() }) { "Backup contains an item with a blank id in $label." }
    require(ids.distinct().size == ids.size) { "Backup contains duplicate ids in $label." }
}

private fun requireReferencedMotorcycles(label: String, references: List<String>, motorcycleIds: Set<String>) {
    references.forEach { motorcycleId ->
        require(motorcycleId.isNotBlank()) { "Backup contains a blank motorcycle reference in $label." }
        require(motorcycleId in motorcycleIds) { "Backup contains a $label item for unknown motorcycle id '$motorcycleId'." }
    }
}

private fun JSONArray.addMotorcycle(value: MotorcycleProfile) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("name", value.name)
            put("plateNumber", value.plateNumber)
            put("vin", value.vin)
            put("purchaseDateIso", value.purchaseDateIso)
            put("currentOdometerKm", value.currentOdometerKm)
            put("notes", value.notes)
            put("imageUris", JSONArray(value.imageUris))
        }
    )
}

private fun JSONArray.addOdometerReading(value: OdometerReading) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("motorcycleId", value.motorcycleId)
            put("dateIso", value.dateIso)
            put("odometerKm", value.odometerKm)
        }
    )
}

private fun JSONArray.addMaintenanceEntry(value: MaintenanceEntry) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("motorcycleId", value.motorcycleId)
            put("title", value.title)
            put("dateIso", value.dateIso)
            put("odometerKm", value.odometerKm)
            put("totalCostPhp", value.totalCostPhp)
            put("notes", value.notes)
            put("costLevelOverride", value.costLevelOverride)
            put("imageUris", JSONArray(value.imageUris))
        }
    )
}

private fun JSONArray.addPartReplacement(value: PartReplacement) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("motorcycleId", value.motorcycleId)
            put("partName", value.partName)
            put("dateIso", value.dateIso)
            put("odometerKm", value.odometerKm)
            put("totalCostPhp", value.totalCostPhp)
            put("notes", value.notes)
            put("costLevelOverride", value.costLevelOverride)
            put("replacementIntervalKm", value.replacementIntervalKm)
            put("warrantyExpiryIso", value.warrantyExpiryIso)
            put("imageUris", JSONArray(value.imageUris))
        }
    )
}

private fun JSONArray.addFuelEntry(value: FuelEntry) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("motorcycleId", value.motorcycleId)
            put("dateIso", value.dateIso)
            put("liters", value.liters)
            put("costPhp", value.costPhp)
            put("distanceKm", value.distanceKm)
        }
    )
}

private fun JSONArray.addReminder(value: ServiceReminder) {
    put(
        JSONObject().apply {
            put("id", value.id)
            put("motorcycleId", value.motorcycleId)
            put("title", value.title)
            put("triggerAtKm", value.triggerAtKm)
            put("triggerAtIsoDate", value.triggerAtIsoDate)
            put("repeatingIntervalKm", value.repeatingIntervalKm)
            put("lastTriggeredAtKm", value.lastTriggeredAtKm)
            put("completionCount", value.completionCount)
            put("lastCompletedAtIsoDate", value.lastCompletedAtIsoDate)
            put("isArchived", value.isArchived)
        }
    )
}

private fun JSONArray?.toMotorcycles(): List<MotorcycleProfile> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            MotorcycleProfile(
                id = item.optString("id"),
                name = item.optString("name"),
                plateNumber = item.optString("plateNumber"),
                vin = item.optString("vin"),
                purchaseDateIso = item.optString("purchaseDateIso"),
                currentOdometerKm = item.optInt("currentOdometerKm"),
                notes = item.optString("notes"),
                imageUris = item.optJSONArray("imageUris").toStringList()
            )
        }
    }
}

private fun JSONArray?.toOdometerReadings(): List<OdometerReading> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            OdometerReading(
                id = item.optString("id"),
                motorcycleId = item.optString("motorcycleId"),
                dateIso = item.optString("dateIso"),
                odometerKm = item.optInt("odometerKm")
            )
        }
    }
}

private fun JSONArray?.toMaintenanceEntries(): List<MaintenanceEntry> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            MaintenanceEntry(
                id = item.optString("id"),
                motorcycleId = item.optString("motorcycleId"),
                title = item.optString("title"),
                dateIso = item.optString("dateIso"),
                odometerKm = item.optInt("odometerKm"),
                totalCostPhp = item.optDouble("totalCostPhp"),
                notes = item.optString("notes"),
                costLevelOverride = item.optStringOrNull("costLevelOverride"),
                imageUris = item.optJSONArray("imageUris").toStringList()
            )
        }
    }
}

private fun JSONArray?.toPartReplacements(): List<PartReplacement> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            PartReplacement(
                id = item.optString("id"),
                motorcycleId = item.optString("motorcycleId"),
                partName = item.optString("partName"),
                dateIso = item.optString("dateIso"),
                odometerKm = item.optInt("odometerKm"),
                totalCostPhp = item.optDouble("totalCostPhp"),
                notes = item.optString("notes"),
                costLevelOverride = item.optStringOrNull("costLevelOverride"),
                replacementIntervalKm = item.optIntOrNull("replacementIntervalKm"),
                warrantyExpiryIso = item.optStringOrNull("warrantyExpiryIso"),
                imageUris = item.optJSONArray("imageUris").toStringList()
            )
        }
    }
}

private fun JSONArray?.toFuelEntries(): List<FuelEntry> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            FuelEntry(
                id = item.optString("id"),
                motorcycleId = item.optString("motorcycleId"),
                dateIso = item.optString("dateIso"),
                liters = item.optDouble("liters"),
                costPhp = item.optDouble("costPhp"),
                distanceKm = item.optDouble("distanceKm")
            )
        }
    }
}

private fun JSONArray?.toReminders(): List<ServiceReminder> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.let { item ->
            ServiceReminder(
                id = item.optString("id"),
                motorcycleId = item.optString("motorcycleId"),
                title = item.optString("title"),
                triggerAtKm = item.optInt("triggerAtKm"),
                triggerAtIsoDate = item.optStringOrNull("triggerAtIsoDate"),
                repeatingIntervalKm = item.optIntOrNull("repeatingIntervalKm"),
                lastTriggeredAtKm = item.optIntOrNull("lastTriggeredAtKm"),
                completionCount = item.optIntOrNull("completionCount") ?: 0,
                lastCompletedAtIsoDate = item.optStringOrNull("lastCompletedAtIsoDate"),
                isArchived = item.optBoolean("isArchived", false)
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = optString(key)
    return value.takeIf { it.isNotBlank() }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject?.toAppSettingsOrNull(): AppSettings? {
    if (this == null) return null

    return AppSettings(
        themeMode = ThemeMode.fromStored(optStringOrNull("themeMode")),
        densityMode = runCatching {
            CardDensityMode.valueOf(optString("densityMode", CardDensityMode.Comfortable.name))
        }.getOrDefault(CardDensityMode.Comfortable),
        showActiveBikeBadge = optBoolean("showActiveBikeBadge", true),
        reduceMotion = optBoolean("reduceMotion", false),
        geminiEnabled = optBoolean("geminiEnabled", false),
        redactSensitiveDataForAi = optBoolean("redactSensitiveDataForAi", true)
    )
}

