package com.thisismine.myapplication.feature.odometer.location

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

interface TripLocationClient {
    fun startTracking(
        onPoint: (GeoPoint) -> Unit,
        onError: (String) -> Unit
    )

    fun stopTracking()
}

object NoOpTripLocationClient : TripLocationClient {
    override fun startTracking(onPoint: (GeoPoint) -> Unit, onError: (String) -> Unit) = Unit

    override fun stopTracking() = Unit
}

