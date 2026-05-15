package com.thisismine.myapplication.feature.fuel.data

import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile

interface FuelAnalyticsRepository {
    fun getCurrentOdometerKm(): Int
    fun getFuelEntries(): List<FuelEntry>
    fun getAllFuelEntries(): List<FuelEntry>
    fun getMotorcycles(): List<MotorcycleProfile>
    fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double)
    fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double)
    fun deleteFuelEntry(id: String)
}
