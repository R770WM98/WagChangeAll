package com.thisismine.myapplication.feature.odometer.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class FusedTripLocationClient(
    context: Context
) : TripLocationClient {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun startTracking(
        onPoint: (GeoPoint) -> Unit,
        onError: (String) -> Unit
    ) {
        if (callback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val trackingCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    onPoint(
                        GeoPoint(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy
                        )
                    )
                }
            }
        }

        callback = trackingCallback

        fusedClient
            .requestLocationUpdates(request, trackingCallback, Looper.getMainLooper())
            .addOnFailureListener { ex ->
                callback = null
                onError(ex.message ?: "Unable to start GPS tracking.")
            }
    }

    override fun stopTracking() {
        val current = callback ?: return
        fusedClient.removeLocationUpdates(current)
        callback = null
    }
}

