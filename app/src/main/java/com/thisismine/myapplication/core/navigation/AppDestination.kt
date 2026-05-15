package com.thisismine.myapplication.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    // Core
    Motorcycles(route = "motorcycles", label = "Bikes", Icons.Filled.TwoWheeler),
    Dashboard(route = "dashboard", label = "Dashboard", Icons.Filled.Dashboard),

    // Tracking & Maintenance
    Odometer(route = "odometer", label = "Odometer", Icons.Filled.Speed),
    MaintenanceLog(route = "maintenance_log", label = "Maintenance", Icons.Filled.Build),

    // Alerts & Insights
    Reminders(route = "reminders", label = "Reminders", Icons.Filled.Notifications),
    Fuel(route = "fuel", label = "Fuel", Icons.Filled.LocalGasStation),

    // Analytics
    Predictive(route = "predictive", label = "Predictive", Icons.Filled.Psychology),
    MechanicSummary(route = "mechanic_summary", label = "Summary", Icons.Filled.Summarize),

    // System
    Settings(route = "settings", label = "Settings", Icons.Filled.Settings)
}
