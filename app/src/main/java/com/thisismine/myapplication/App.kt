package com.thisismine.myapplication

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.core.navigation.AppDestination
import com.thisismine.myapplication.core.navigation.AppNavGraph
import com.thisismine.myapplication.core.ui.AppCardShell
import com.thisismine.myapplication.core.ui.CardDensityMode
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.toDensity
import com.thisismine.myapplication.data.local.WagChangeDatabase
import com.thisismine.myapplication.data.repository.RoomWagChangeRepository
import com.thisismine.myapplication.settings.AppDataTransferService
import com.thisismine.myapplication.settings.AppSettings
import com.thisismine.myapplication.settings.AppSettingsStore
import com.thisismine.myapplication.settings.ui.AiSettingsCard
import com.thisismine.myapplication.ui.theme.ThemeMode
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WagChangeAllApp(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val wagChangeRepository = remember {
        RoomWagChangeRepository(WagChangeDatabase.create(appContext))
    }
    val activeMotorcycle by wagChangeRepository.activeMotorcycleFlow.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val primaryDestinations = listOf(
        AppDestination.Motorcycles,
        AppDestination.Dashboard,
        AppDestination.MaintenanceLog,
        AppDestination.Reminders
    )
    val secondaryDestinations = AppDestination.entries.filterNot { it in primaryDestinations }
    val isOnSecondaryDestination = secondaryDestinations.any { it.route == currentRoute }

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(AppDestination.Motorcycles.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalCardDensity provides settings.densityMode.toDensity()) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("WagChangeAll", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = AppDestination.entries.firstOrNull { it.route == currentRoute }?.label
                                        ?: "Maintenance and tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (settings.showActiveBikeBadge) {
                                AppCardShell(
                                    tonal = true,
                                    fullWidth = false,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Active bike: ${activeMotorcycle?.name ?: "No Active Motorcycle"}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    primaryDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigateTo(destination) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors()
                        )
                    }
                    NavigationBarItem(
                        selected = isOnSecondaryDestination || showMoreSheet,
                        onClick = { showMoreSheet = true },
                        icon = {
                            Icon(
                                Icons.Filled.MoreHoriz,
                                contentDescription = "Open more destinations"
                            )
                        },
                        label = {
                            Text(
                                text = "More",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors()
                    )
                }
            }
        ) { innerPadding ->
            AppNavGraph(
                navController = navController,
                wagChangeRepository = wagChangeRepository,
                modifier = Modifier.padding(innerPadding)
            )
        }

        if (showMoreSheet) {
            ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "More destinations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Current: ${AppDestination.entries.firstOrNull { it.route == currentRoute }?.label ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    secondaryDestinations.forEach { destination ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = currentRoute == destination.route,
                            onClick = {
                                showMoreSheet = false
                                if (destination == AppDestination.Settings) {
                                    showSettingsDialog = true
                                } else {
                                    navigateTo(destination)
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = null)
                            },
                            badge = {
                                if (currentRoute == destination.route) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AppSettingsDialog(
                settings = settings,
                onDismiss = { showSettingsDialog = false },
                onSettingsChange = onSettingsChange,
                sharedPreferences = appContext.getSharedPreferences(AppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE),
                wagChangeRepository = wagChangeRepository
            )
        }
    }
}

@Composable
private fun AppSettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    sharedPreferences: android.content.SharedPreferences,
    wagChangeRepository: WagChangeRepository
) {
    val context = LocalContext.current
    val transferService = remember { AppDataTransferService() }
    var transferMessage by remember { mutableStateOf<String?>(null) }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingCsvFileName by remember { mutableStateOf("export.csv") }

    fun writeTextToUri(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(content)
        } ?: throw IOException("Unable to open destination file.")
    }

    fun readTextFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText()
        } ?: throw IOException("Unable to open source file.")
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val content = pendingBackupJson
        pendingBackupJson = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult

        try {
            writeTextToUri(uri, content)
            transferMessage = context.getString(R.string.backup_export_success)
        } catch (error: Exception) {
            transferMessage = context.getString(
                R.string.backup_export_failed,
                error.message ?: error::class.java.simpleName
            )
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvContent
        val fileName = pendingCsvFileName
        pendingCsvContent = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult

        try {
            writeTextToUri(uri, content)
            transferMessage = if (fileName.contains("maintenance", ignoreCase = true)) {
                context.getString(R.string.maintenance_csv_export_success)
            } else {
                context.getString(R.string.fuel_csv_export_success)
            }
        } catch (error: Exception) {
            transferMessage = if (fileName.contains("maintenance", ignoreCase = true)) {
                context.getString(
                    R.string.maintenance_csv_export_failed,
                    error.message ?: error::class.java.simpleName
                )
            } else {
                context.getString(
                    R.string.fuel_csv_export_failed,
                    error.message ?: error::class.java.simpleName
                )
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val imported = transferService.fromBackupJson(readTextFromUri(uri))
            wagChangeRepository.importBackupSnapshot(imported.snapshot)
            imported.settings?.let { restoredSettings ->
                AppSettingsStore.save(sharedPreferences, restoredSettings)
                onSettingsChange(restoredSettings)
            }
            transferMessage = context.getString(R.string.backup_import_success)
        } catch (error: Exception) {
            transferMessage = context.getString(
                R.string.backup_import_failed,
                error.message ?: error::class.java.simpleName
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App settings") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Theme mode", style = MaterialTheme.typography.titleSmall)
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(mode.displayLabel())
                        RadioButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSettingsChange(settings.copy(themeMode = mode)) }
                        )
                    }
                }

                Text("Layout density", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSettingsChange(settings.copy(densityMode = CardDensityMode.Compact)) }
                    ) { Text("Compact") }
                    OutlinedButton(
                        onClick = { onSettingsChange(settings.copy(densityMode = CardDensityMode.Comfortable)) }
                    ) { Text("Comfort") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Show active motorcycle badge")
                    Switch(
                        checked = settings.showActiveBikeBadge,
                        onCheckedChange = { checked -> onSettingsChange(settings.copy(showActiveBikeBadge = checked)) }
                    )
                }

                AiSettingsCard(
                    appSettings = settings,
                    sharedPreferences = sharedPreferences,
                    onSettingsChanged = onSettingsChange
                )

                Text(stringResource(R.string.data_section), style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pendingBackupJson = transferService.toBackupJson(
                                snapshot = wagChangeRepository.exportBackupSnapshot(),
                                settings = settings
                            )
                            backupExportLauncher.launch("wagchangeall-backup.json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.export_backup)) }

                    OutlinedButton(
                        onClick = { backupImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.import_backup)) }

                    OutlinedButton(
                        onClick = {
                            pendingCsvContent = wagChangeRepository.exportMaintenanceCsv()
                            pendingCsvFileName = "maintenance.csv"
                            csvExportLauncher.launch("maintenance.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.maintenance_csv)) }

                    OutlinedButton(
                        onClick = {
                            pendingCsvContent = wagChangeRepository.exportFuelCsv()
                            pendingCsvFileName = "fuel.csv"
                            csvExportLauncher.launch("fuel.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.fuel_csv)) }
                }

                transferMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}