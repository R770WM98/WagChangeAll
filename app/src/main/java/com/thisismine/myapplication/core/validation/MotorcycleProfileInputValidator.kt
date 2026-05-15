package com.thisismine.myapplication.core.validation

data class ValidatedMotorcycleInput(
    val name: String,
    val plateNumber: String,
    val vin: String,
    val purchaseDateIso: String,
    val odometerKm: Int,
    val notes: String
)

sealed class MotorcycleProfileValidationResult {
    data class Success(val value: ValidatedMotorcycleInput) : MotorcycleProfileValidationResult()
    data class Error(val message: String) : MotorcycleProfileValidationResult()
}

fun validateMotorcycleProfileInput(
    name: String,
    plateNumber: String,
    vin: String,
    purchaseDateIso: String,
    odometerInput: String,
    notes: String,
    invalidOdometerMessage: String
): MotorcycleProfileValidationResult {
    val trimmedName = name.trim()
    val trimmedPlate = plateNumber.trim()
    val trimmedVin = vin.trim()
    val trimmedDate = purchaseDateIso.trim()
    val odometerKm = odometerInput.trim().toIntOrNull()

    if (trimmedName.isEmpty() || trimmedPlate.isEmpty() || trimmedVin.isEmpty() || trimmedDate.isEmpty()) {
        return MotorcycleProfileValidationResult.Error("Name, plate number, VIN, and purchase date are required.")
    }
    if (!trimmedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
        return MotorcycleProfileValidationResult.Error("Purchase date must use yyyy-MM-dd.")
    }
    if (odometerKm == null || odometerKm < 0) {
        return MotorcycleProfileValidationResult.Error(invalidOdometerMessage)
    }

    return MotorcycleProfileValidationResult.Success(
        ValidatedMotorcycleInput(
            name = trimmedName,
            plateNumber = trimmedPlate,
            vin = trimmedVin,
            purchaseDateIso = trimmedDate,
            odometerKm = odometerKm,
            notes = notes.trim()
        )
    )
}

