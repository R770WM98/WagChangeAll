package com.thisismine.myapplication.feature.odometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thisismine.myapplication.feature.odometer.data.OdometerRepository
import com.thisismine.myapplication.feature.odometer.location.TripLocationClient

class OdometerViewModelFactory(
    private val repository: OdometerRepository,
    private val tripLocationClient: TripLocationClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OdometerViewModel::class.java)) {
            return OdometerViewModel(repository, tripLocationClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
