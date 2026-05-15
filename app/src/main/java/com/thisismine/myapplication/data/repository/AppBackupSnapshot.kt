package com.thisismine.myapplication.data.repository

import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.domain.model.PartReplacement
import com.thisismine.myapplication.domain.model.ServiceReminder

data class AppBackupSnapshot(
    val schemaVersion: Int = SCHEMA_VERSION,
    val activeMotorcycleId: String,
    val motorcycles: List<MotorcycleProfile>,
    val odometerReadings: List<OdometerReading>,
    val maintenanceEntries: List<MaintenanceEntry>,
    val partReplacements: List<PartReplacement>,
    val fuelEntries: List<FuelEntry>,
    val reminders: List<ServiceReminder>
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

