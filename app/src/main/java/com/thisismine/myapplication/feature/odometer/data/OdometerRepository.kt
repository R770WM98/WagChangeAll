package com.thisismine.myapplication.feature.odometer.data

import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading

interface OdometerRepository {
    fun getCurrentOdometerKm(): Int
    fun getReadings(): List<OdometerReading>
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
    )
    fun updateMotorcycle(
        motorcycleId: String,
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String
    )
    fun addReading(odometerKm: Int)
}
