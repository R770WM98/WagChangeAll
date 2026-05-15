package com.thisismine.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM motorcycles ORDER BY name")
    fun getAll(): List<MotorcycleEntity>

    @Query("SELECT * FROM motorcycles WHERE id = :id LIMIT 1")
    fun getById(id: String): MotorcycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MotorcycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<MotorcycleEntity>)

    @Update
    fun update(entity: MotorcycleEntity)

    @Delete
    fun delete(entity: MotorcycleEntity)

    @Query("UPDATE motorcycles SET current_odometer_km = :odometerKm WHERE id = :motorcycleId")
    fun updateCurrentOdometer(motorcycleId: String, odometerKm: Int)

    @Query("DELETE FROM motorcycles")
    fun clearAll()
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_readings WHERE motorcycle_id = :motorcycleId")
    fun getByMotorcycleId(motorcycleId: String): List<OdometerReadingEntity>

    @Query("SELECT * FROM odometer_readings")
    fun getAll(): List<OdometerReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: OdometerReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<OdometerReadingEntity>)

    @Query("DELETE FROM odometer_readings")
    fun clearAll()
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_entries WHERE motorcycle_id = :motorcycleId ORDER BY date_iso DESC")
    fun getByMotorcycleId(motorcycleId: String): List<MaintenanceEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MaintenanceEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<MaintenanceEntryEntity>)

    @Update
    fun update(entity: MaintenanceEntryEntity)

    @Delete
    fun delete(entity: MaintenanceEntryEntity)

    @Query("SELECT * FROM maintenance_entries WHERE id = :id LIMIT 1")
    fun getById(id: String): MaintenanceEntryEntity?

    @Query("SELECT * FROM maintenance_entries")
    fun getAll(): List<MaintenanceEntryEntity>

    @Query("DELETE FROM maintenance_entries")
    fun clearAll()
}

@Dao
interface PartDao {
    @Query("SELECT * FROM part_replacements WHERE motorcycle_id = :motorcycleId ORDER BY date_iso DESC")
    fun getByMotorcycleId(motorcycleId: String): List<PartReplacementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PartReplacementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<PartReplacementEntity>)

    @Update
    fun update(entity: PartReplacementEntity)

    @Delete
    fun delete(entity: PartReplacementEntity)

    @Query("SELECT * FROM part_replacements WHERE id = :id LIMIT 1")
    fun getById(id: String): PartReplacementEntity?

    @Query("SELECT * FROM part_replacements")
    fun getAll(): List<PartReplacementEntity>

    @Query("DELETE FROM part_replacements")
    fun clearAll()
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE motorcycle_id = :motorcycleId")
    fun getByMotorcycleId(motorcycleId: String): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries")
    fun getAll(): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    fun getById(id: String): FuelEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: FuelEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<FuelEntryEntity>)

    @Update
    fun update(entity: FuelEntryEntity)

    @Delete
    fun delete(entity: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries")
    fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM service_reminders WHERE motorcycle_id = :motorcycleId")
    fun getByMotorcycleId(motorcycleId: String): List<ServiceReminderEntity>

    @Query("SELECT * FROM service_reminders")
    fun getAll(): List<ServiceReminderEntity>

    @Query("SELECT * FROM service_reminders WHERE id = :id LIMIT 1")
    fun getById(id: String): ServiceReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ServiceReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<ServiceReminderEntity>)

    @Update
    fun update(entity: ServiceReminderEntity)

    @Delete
    fun delete(entity: ServiceReminderEntity)

    @Query("DELETE FROM service_reminders")
    fun clearAll()
}

@Dao
interface AppStateDao {
    @Query("SELECT value FROM app_state WHERE `key` = :key LIMIT 1")
    fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: AppStateEntity)

    @Query("DELETE FROM app_state")
    fun clearAll()
}
