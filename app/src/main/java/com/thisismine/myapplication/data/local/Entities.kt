package com.thisismine.myapplication.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "motorcycles")
data class MotorcycleEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "plate_number") val plateNumber: String,
    val vin: String,
    @ColumnInfo(name = "purchase_date_iso") val purchaseDateIso: String,
    @ColumnInfo(name = "current_odometer_km") val currentOdometerKm: Int,
    val notes: String,
    @ColumnInfo(name = "image_uris") val imageUris: String
)

@Entity(
    tableName = "odometer_readings",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycle_id")]
)
data class OdometerReadingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "motorcycle_id") val motorcycleId: String,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "odometer_km") val odometerKm: Int
)

@Entity(
    tableName = "maintenance_entries",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycle_id")]
)
data class MaintenanceEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "motorcycle_id") val motorcycleId: String,
    val title: String,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "odometer_km") val odometerKm: Int,
    @ColumnInfo(name = "total_cost_php") val totalCostPhp: Double,
    val notes: String,
    @ColumnInfo(name = "cost_level_override") val costLevelOverride: String? = null,
    @ColumnInfo(name = "image_uris") val imageUris: String = ""
)

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycle_id")]
)
data class FuelEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "motorcycle_id") val motorcycleId: String,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    val liters: Double,
    @ColumnInfo(name = "cost_php") val costPhp: Double,
    @ColumnInfo(name = "distance_km") val distanceKm: Double
)

@Entity(
    tableName = "service_reminders",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycle_id")]
)
data class ServiceReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "motorcycle_id") val motorcycleId: String,
    val title: String,
    @ColumnInfo(name = "trigger_at_km") val triggerAtKm: Int,
    @ColumnInfo(name = "trigger_at_iso_date") val triggerAtIsoDate: String?,
    @ColumnInfo(name = "repeating_interval_km") val repeatingIntervalKm: Int? = null,
    @ColumnInfo(name = "last_triggered_at_km") val lastTriggeredAtKm: Int? = null,
    @ColumnInfo(name = "completion_count") val completionCount: Int = 0,
    @ColumnInfo(name = "last_completed_at_iso_date") val lastCompletedAtIsoDate: String? = null,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "part_replacements",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycle_id")]
)
data class PartReplacementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "motorcycle_id") val motorcycleId: String,
    @ColumnInfo(name = "part_name") val partName: String,
    @ColumnInfo(name = "date_iso") val dateIso: String,
    @ColumnInfo(name = "odometer_km") val odometerKm: Int,
    @ColumnInfo(name = "total_cost_php") val totalCostPhp: Double,
    val notes: String,
    @ColumnInfo(name = "cost_level_override") val costLevelOverride: String? = null,
    @ColumnInfo(name = "replacement_interval_km") val replacementIntervalKm: Int? = null,
    @ColumnInfo(name = "warranty_expiry_iso") val warrantyExpiryIso: String? = null,
    @ColumnInfo(name = "image_uris") val imageUris: String = ""
)
