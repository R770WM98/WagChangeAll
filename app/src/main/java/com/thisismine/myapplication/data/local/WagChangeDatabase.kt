package com.thisismine.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MotorcycleEntity::class,
        OdometerReadingEntity::class,
        MaintenanceEntryEntity::class,
        FuelEntryEntity::class,
        ServiceReminderEntity::class,
        PartReplacementEntity::class,
        AppStateEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class WagChangeDatabase : RoomDatabase() {
    abstract fun motorcycleDao(): MotorcycleDao
    abstract fun odometerDao(): OdometerDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fuelDao(): FuelDao
    abstract fun reminderDao(): ReminderDao
    abstract fun partDao(): PartDao
    abstract fun appStateDao(): AppStateDao

    companion object {
        fun create(context: Context): WagChangeDatabase {
            return Room.databaseBuilder(
                context,
                WagChangeDatabase::class.java,
                "wagchange.db"
            )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
