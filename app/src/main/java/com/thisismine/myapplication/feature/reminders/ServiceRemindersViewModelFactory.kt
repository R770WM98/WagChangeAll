package com.thisismine.myapplication.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thisismine.myapplication.feature.reminders.data.ReminderRepository
import com.thisismine.myapplication.feature.reminders.notifications.ReminderNotificationScheduler

class ServiceRemindersViewModelFactory(
    private val repository: ReminderRepository,
    private val notificationScheduler: ReminderNotificationScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceRemindersViewModel::class.java)) {
            return ServiceRemindersViewModel(repository, notificationScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
