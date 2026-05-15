package com.thisismine.myapplication.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thisismine.myapplication.feature.fuel.data.FuelAnalyticsRepository

class FuelAnalyticsViewModelFactory(
    private val repository: FuelAnalyticsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FuelAnalyticsViewModel::class.java)) {
            return FuelAnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

