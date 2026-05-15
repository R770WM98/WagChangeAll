package com.thisismine.myapplication.feature.odometer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.OdometerReading
import com.thisismine.myapplication.feature.odometer.data.WagChangeOdometerRepository
import com.thisismine.myapplication.feature.odometer.location.FusedTripLocationClient

@Suppress("UNUSED_CHANGED_VALUE")
@Composable
fun OdometerTrackingScreen(
    wagChangeRepository: WagChangeRepository
) {
    val context = LocalContext.current
    val locationClient = remember { FusedTripLocationClient(context.applicationContext) }
    val vm: OdometerViewModel = viewModel(
        factory = OdometerViewModelFactory(
            repository = WagChangeOdometerRepository(wagChangeRepository),
            tripLocationClient = locationClient
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val activeBike = uiState.motorcycles.firstOrNull { it.id == uiState.activeMotorcycleId }

    var hasFineLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var editorBike by remember { mutableStateOf<com.thisismine.myapplication.domain.model.MotorcycleProfile?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasFineLocationPermission = granted
        if (granted) vm.startTripTracking()
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopTripTracking() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ActiveBikeCard(
                name = activeBike?.name ?: "No Active Motorcycle",
                plateNumber = activeBike?.plateNumber ?: "",
                odometerKm = uiState.currentOdometerKm,
                onEdit = { editorBike = activeBike },
                enabled = activeBike != null
            )
        }
        item {
            GpsTripCard(
                hasPermission = hasFineLocationPermission,
                isTracking = uiState.isTripTracking,
                isPaused = uiState.isTripPaused,
                trackedDistanceKm = uiState.trackedDistanceKm,
                error = uiState.tripError,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onStart = vm::startTripTracking,
                onPause = vm::pauseTripTracking,
                onResume = vm::resumeTripTracking,
                onStop = vm::stopTripTracking,
                onApply = vm::applyTrackedDistanceToOdometer
            )
        }
        item {
            AddReadingCard(
                input = uiState.newReadingInput,
                inputError = uiState.inputError,
                onInputChange = vm::onReadingInputChange,
                onSave = vm::saveReading
            )
        }
        item { Text(text = "Reading History", style = MaterialTheme.typography.titleMedium) }
        if (uiState.readings.isEmpty()) {
            item { Text(text = "No odometer readings yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(uiState.readings, key = { it.id }) { reading ->
                OdometerReadingCard(reading = reading)
            }
        }
    }

    if (editorBike != null) {
        ActiveBikeEditorDialog(
            bike = editorBike,
            error = uiState.motorcycleFormError,
            message = uiState.motorcycleFormMessage,
            onSave = { name, plateNumber, vin, purchaseDateIso, odometer, notes ->
                if (vm.updateActiveMotorcycle(name, plateNumber, vin, purchaseDateIso, odometer, notes)) {
                    editorBike = null
                }
            },
            onCancel = { editorBike = null }
        )
    }
}

@Composable
private fun ActiveBikeCard(
    name: String,
    plateNumber: String,
    odometerKm: Int,
    onEdit: () -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Active Motorcycle", style = MaterialTheme.typography.titleMedium)
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            if (plateNumber.isNotBlank()) {
                Text(text = "Plate: $plateNumber", style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "Current Odometer: $odometerKm km", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onEdit, enabled = enabled) { Text("Edit Active Bike") }
        }
    }
}

@Composable
private fun GpsTripCard(
    hasPermission: Boolean,
    isTracking: Boolean,
    isPaused: Boolean,
    trackedDistanceKm: Double,
    error: String?,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onApply: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "GPS Trip Tracking", style = MaterialTheme.typography.titleMedium)
            val statusText = when {
                !isTracking -> "Status: Idle"
                isPaused -> "Status: Paused"
                else -> "Status: Tracking"
            }
            Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Tracked trip: ${"%.2f".format(trackedDistanceKm)} km",
                style = MaterialTheme.typography.bodyLarge
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !hasPermission -> Button(onClick = onRequestPermission) { Text("Grant GPS") }
                    !isTracking -> Button(onClick = onStart) { Text("Start Trip") }
                    isPaused -> {
                        Button(onClick = onResume) { Text("Resume") }
                        Button(onClick = onStop) { Text("Stop") }
                    }
                    else -> {
                        Button(onClick = onPause) { Text("Pause") }
                        Button(onClick = onStop) { Text("Stop") }
                    }
                }
                Button(onClick = onApply, enabled = trackedDistanceKm >= 0.1) {
                    Text("Apply to Odometer")
                }
            }
        }
    }
}

@Composable
private fun AddReadingCard(
    input: String,
    inputError: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Add New Reading", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Odometer (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = inputError != null
            )
            if (inputError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = inputError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveBikeEditorDialog(
    bike: com.thisismine.myapplication.domain.model.MotorcycleProfile?,
    error: String?,
    message: String?,
    onSave: (String, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(bike?.name.orEmpty()) }
    var plateNumber by remember { mutableStateOf(bike?.plateNumber.orEmpty()) }
    var vin by remember { mutableStateOf(bike?.vin.orEmpty()) }
    var purchaseDateIso by remember { mutableStateOf(bike?.purchaseDateIso.orEmpty()) }
    var odometer by remember { mutableStateOf(bike?.currentOdometerKm?.toString().orEmpty()) }
    var notes by remember { mutableStateOf(bike?.notes.orEmpty()) }

    LaunchedEffect(bike?.id) {
        name = bike?.name.orEmpty()
        plateNumber = bike?.plateNumber.orEmpty()
        vin = bike?.vin.orEmpty()
        purchaseDateIso = bike?.purchaseDateIso.orEmpty()
        odometer = bike?.currentOdometerKm?.toString().orEmpty()
        notes = bike?.notes.orEmpty()
    }

    BasicAlertDialog(onDismissRequest = onCancel, modifier = Modifier.fillMaxWidth(0.9f)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Edit Active Motorcycle", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(plateNumber, onValueChange = { plateNumber = it }, label = { Text("Plate Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(vin, onValueChange = { vin = it }, label = { Text("VIN") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    purchaseDateIso,
                    onValueChange = { purchaseDateIso = it },
                    label = { Text("Purchase date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    odometer,
                    onValueChange = { odometer = it },
                    label = { Text("Current odometer km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(name, plateNumber, vin, purchaseDateIso, odometer, notes) }) { Text("Save") }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun OdometerReadingCard(reading: OdometerReading) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "${reading.odometerKm} km", style = MaterialTheme.typography.titleMedium)
            Text(text = "Date: ${reading.dateIso}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
