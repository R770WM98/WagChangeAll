package com.thisismine.myapplication.feature.odometer.data

import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.domain.model.OdometerReading
import java.time.LocalDate

class WagChangeOdometerRepository(
    private val wagChangeRepository: WagChangeRepository
) : OdometerRepository {
    override fun getCurrentOdometerKm(): Int = wagChangeRepository.getProfile().currentOdometerKm

    override fun getReadings(): List<OdometerReading> = wagChangeRepository.getOdometerReadings()

    override fun getMotorcycles(): List<MotorcycleProfile> = wagChangeRepository.getMotorcycles()

    override fun getActiveMotorcycleId(): String = wagChangeRepository.getActiveMotorcycleId()

    override fun setActiveMotorcycle(motorcycleId: String) {
        wagChangeRepository.setActiveMotorcycle(motorcycleId)
    }

    override fun addMotorcycle(
        name: String,
        plateNumber: String,
        vin: String,
        purchaseDateIso: String,
        currentOdometerKm: Int,
        notes: String,
        imageUris: List<String>
    ) {
        wagChangeRepository.addMotorcycle(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            currentOdometerKm = currentOdometerKm,
            notes = notes,
            imageUris = imageUris
        )
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
        wagChangeRepository.updateMotorcycle(
            motorcycleId = motorcycleId,
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            currentOdometerKm = currentOdometerKm,
            notes = notes
        )
    }

    override fun addReading(odometerKm: Int) {
        wagChangeRepository.addOdometerReading(
            odometerKm = odometerKm,
            dateIso = LocalDate.now().toString()
        )
    }
}
