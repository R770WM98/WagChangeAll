package com.thisismine.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thisismine.myapplication.core.ui.LocalMotionScheme
import com.thisismine.myapplication.core.ui.navigationEnter
import com.thisismine.myapplication.core.ui.navigationExit
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.dashboard.DashboardScreen
import com.thisismine.myapplication.feature.fuel.FuelTrackerScreen
import com.thisismine.myapplication.feature.maintenance.MaintenanceLogScreen
import com.thisismine.myapplication.feature.mechanic.MechanicSummaryScreen
import com.thisismine.myapplication.feature.motorcycles.MotorcyclesScreen
import com.thisismine.myapplication.feature.odometer.OdometerTrackingScreen
import com.thisismine.myapplication.feature.predictive.PredictiveRulesScreen
import com.thisismine.myapplication.feature.reminders.data.WagChangeReminderRepository
import com.thisismine.myapplication.feature.reminders.ServiceRemindersScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisismine.myapplication.ai.AiRepository
import com.thisismine.myapplication.ai.GeminiAiClient
import com.thisismine.myapplication.core.ui.FeatureTemplateScreen
import android.content.Context
import com.thisismine.myapplication.settings.AppSettingsStore
import com.thisismine.myapplication.settings.GeminiApiKeyStore
import com.thisismine.myapplication.feature.predictive.PredictiveRulesViewModel
import com.thisismine.myapplication.feature.mechanic.MechanicSummaryViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    wagChangeRepository: WagChangeRepository,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionScheme.current

    fun routeIndex(route: String?): Int {
        val index = AppDestination.entries.indexOfFirst { it.route == route }
        return if (index >= 0) index else 0
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Motorcycles.route,
        modifier = modifier,
        enterTransition = {
            val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
            motion.navigationEnter(forward = forward)
        },
        exitTransition = {
            val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
            motion.navigationExit(forward = forward)
        },
        popEnterTransition = {
            val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
            motion.navigationEnter(forward = !forward)
        },
        popExitTransition = {
            val forward = routeIndex(targetState.destination.route) >= routeIndex(initialState.destination.route)
            motion.navigationExit(forward = !forward)
        }
    ) {
        composable(AppDestination.Motorcycles.route) {
            MotorcyclesScreen(wagChangeRepository = wagChangeRepository)
        }
        composable(AppDestination.Dashboard.route) {
            DashboardScreen(
                wagChangeRepository = wagChangeRepository,
                onOpenMotorcycles = { navController.navigate(AppDestination.Motorcycles.route) },
                onOpenOdometer = { navController.navigate(AppDestination.Odometer.route) },
                onOpenMaintenance = { navController.navigate(AppDestination.MaintenanceLog.route) },
                onOpenReminders = { navController.navigate(AppDestination.Reminders.route) },
                onOpenFuel = { navController.navigate(AppDestination.Fuel.route) }
            )
        }
        composable(AppDestination.MaintenanceLog.route) {
            MaintenanceLogScreen(
                wagChangeRepository = wagChangeRepository
            )
        }
        composable(AppDestination.Odometer.route) {
            OdometerTrackingScreen(wagChangeRepository = wagChangeRepository)
        }
        composable(AppDestination.Reminders.route) {
            ServiceRemindersScreen(wagChangeRepository = wagChangeRepository)
        }
        composable(AppDestination.Fuel.route) {
            FuelTrackerScreen(wagChangeRepository = wagChangeRepository)
        }
        composable(AppDestination.Predictive.route) {
            val activeBikeId = wagChangeRepository.getActiveMotorcycleId()
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPrefs = context.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            val geminiPrefs = context.getSharedPreferences(GeminiApiKeyStore.PREFS_NAME, Context.MODE_PRIVATE)
            val settings = AppSettingsStore.load(sharedPrefs)
            val geminiApiKey = GeminiApiKeyStore.load(geminiPrefs)
            if (settings.geminiEnabled && geminiApiKey.isNotEmpty()) {
                val geminiClient = GeminiAiClient(geminiApiKey)
                val aiRepo = AiRepository(geminiClient, wagChangeRepository)
                val reminderRepo = WagChangeReminderRepository(wagChangeRepository)
                val viewModel: PredictiveRulesViewModel = viewModel(
                    factory = simpleViewModelFactory {
                        PredictiveRulesViewModel(aiRepo, reminderRepo, activeBikeId)
                    }
                )
                PredictiveRulesScreen(viewModel)
            } else {
                FeatureTemplateScreen(
                    title = "Predictive Rules",
                    description = "AI-powered maintenance predictions. Enable Gemini AI in settings to use this feature.",
                    checkpoints = listOf("Enable Gemini API key in settings")
                )
            }
        }
        composable(AppDestination.MechanicSummary.route) {
            val activeBikeId = wagChangeRepository.getActiveMotorcycleId()
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPrefs = context.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            val geminiPrefs = context.getSharedPreferences(GeminiApiKeyStore.PREFS_NAME, Context.MODE_PRIVATE)
            val settings = AppSettingsStore.load(sharedPrefs)
            val geminiApiKey = GeminiApiKeyStore.load(geminiPrefs)
            if (settings.geminiEnabled && geminiApiKey.isNotEmpty()) {
                val geminiClient = GeminiAiClient(geminiApiKey)
                val aiRepo = AiRepository(geminiClient, wagChangeRepository)
                val viewModel: MechanicSummaryViewModel = viewModel(
                    factory = simpleViewModelFactory {
                        MechanicSummaryViewModel(aiRepo, activeBikeId)
                    }
                )
                MechanicSummaryScreen(viewModel)
            } else {
                FeatureTemplateScreen(
                    title = "Mechanic Summary",
                    description = "AI-powered service history summaries. Enable Gemini AI in settings to use this feature.",
                    checkpoints = listOf("Enable Gemini API key in settings")
                )
            }
        }
        composable(AppDestination.Settings.route) {
            FeatureTemplateScreen(
                title = "Settings",
                description = "Open app settings from the top bar settings icon.",
                checkpoints = listOf("Click the settings icon in the top-right corner")
            )
        }
    }
}

private inline fun <reified T : ViewModel> simpleViewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            return create() as VM
        }
    }
}

