package com.thisismine.myapplication.feature.motorcycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thisismine.myapplication.data.repository.WagChangeRepository

class MotorcyclesViewModelFactory(
    private val repository: WagChangeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MotorcyclesViewModel::class.java)) {
            return MotorcyclesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

