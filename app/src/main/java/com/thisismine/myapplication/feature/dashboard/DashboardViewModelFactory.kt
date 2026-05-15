package com.thisismine.myapplication.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler

class DashboardViewModelFactory(
    private val repository: WagChangeRepository,
    private val notificationScheduler: ReminderNotificationScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, notificationScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
