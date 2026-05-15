package com.thisismine.myapplication.feature.fuel.data

import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.FuelEntry
import com.thisismine.myapplication.domain.model.MotorcycleProfile

class WagChangeFuelAnalyticsRepository(
    private val wagChangeRepository: WagChangeRepository
) : FuelAnalyticsRepository {
    override fun getCurrentOdometerKm(): Int = wagChangeRepository.getProfile().currentOdometerKm

    override fun getFuelEntries(): List<FuelEntry> = wagChangeRepository.getFuelEntries()

    override fun getAllFuelEntries(): List<FuelEntry> = wagChangeRepository.getAllFuelEntries()

    override fun getMotorcycles(): List<MotorcycleProfile> = wagChangeRepository.getMotorcycles()

    override fun addFuelEntry(dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        wagChangeRepository.addFuelEntry(dateIso, liters, costPhp, distanceKm)
    }

    override fun updateFuelEntry(id: String, dateIso: String, liters: Double, costPhp: Double, distanceKm: Double) {
        wagChangeRepository.updateFuelEntry(id, dateIso, liters, costPhp, distanceKm)
    }

    override fun deleteFuelEntry(id: String) {
        wagChangeRepository.deleteFuelEntry(id)
    }
}
