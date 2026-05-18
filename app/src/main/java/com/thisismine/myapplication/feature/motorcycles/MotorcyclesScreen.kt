package com.thisismine.myapplication.feature.motorcycles

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.thisismine.myapplication.core.ui.CardHeaderRow
import com.thisismine.myapplication.core.ui.CompactIconAction
import com.thisismine.myapplication.core.ui.FormDialogShell
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.StatusChip
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.domain.model.MotorcycleProfile
import com.thisismine.myapplication.feature.motorcycles.ui.MotorcycleProfileFormFields
import com.thisismine.myapplication.ui.theme.MyApplicationTheme

@Composable
fun MotorcyclesScreen(
    wagChangeRepository: WagChangeRepository
) {
    val vm: MotorcyclesViewModel = viewModel(
        factory = MotorcyclesViewModelFactory(repository = wagChangeRepository)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val pendingDelete = uiState.motorcycles.firstOrNull { it.id == uiState.pendingDeleteMotorcycleId }

    val context = LocalContext.current

    var showForm by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var purchaseDateIso by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf(listOf<String>()) }
    var imageTargetMotorcycleId by remember { mutableStateOf<String?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            val imageText = uri.toString()
            val targetId = imageTargetMotorcycleId
            if (targetId == null) {
                selectedImageUris = selectedImageUris + imageText
            } else {
                vm.addMotorcyclePhoto(targetId, imageText)
            }
        }
    }

    fun resetForm() {
        showForm = false
        editId = null
        name = ""
        plateNumber = ""
        vin = ""
        purchaseDateIso = ""
        odometer = ""
        notes = ""
        selectedImageUris = emptyList()
    }

    val density = LocalCardDensity.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(density.listContentPadding),
        verticalArrangement = Arrangement.spacedBy(density.sectionSpacing)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bike Garage", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showForm = true; editId = null }) { Text("Add Bike") }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.filterQuery,
                onValueChange = vm::onFilterQueryChange,
                label = { Text("Search bikes") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(uiState.filteredMotorcycles, key = { it.id }) { bike ->
            MotorcycleRowCard(
                bike = bike,
                isActive = bike.id == uiState.activeMotorcycleId,
                cardPadding = density.cardPadding,
                rowSpacing = density.rowSpacing,
                onSelect = { vm.selectMotorcycle(bike.id) },
                onEdit = {
                    showForm = true
                    editId = bike.id
                    name = bike.name
                    plateNumber = bike.plateNumber
                    vin = bike.vin
                    purchaseDateIso = bike.purchaseDateIso
                    odometer = bike.currentOdometerKm.toString()
                    notes = bike.notes
                    selectedImageUris = bike.imageUris
                },
                onDelete = { vm.requestDeleteMotorcycle(bike.id) },
                onRemoveFirstPicture = {
                    if (bike.imageUris.isNotEmpty()) vm.removeMotorcyclePhoto(bike.id, 0)
                },
                onPickImage = {
                    imageTargetMotorcycleId = bike.id
                    imagePicker.launch(arrayOf("image/*"))
                },
                onPreviewImage = { uri -> previewImageUri = uri }
            )
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDeleteMotorcycle,
            title = { Text("Delete motorcycle?") },
            text = { Text("This will delete ${pendingDelete.name} and its stored records.") },
            confirmButton = {
                TextButton(onClick = vm::confirmDeleteMotorcycle) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDeleteMotorcycle) { Text("Cancel") }
            }
        )
    }

    if (showForm) {
        MotorcycleFormDialog(
            editId = editId,
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometer = odometer,
            notes = notes,
            selectedImageUris = selectedImageUris,
            onNameChange = { name = it },
            onPlateNumberChange = { plateNumber = it },
            onVinChange = { vin = it },
            onPurchaseDateChange = { purchaseDateIso = it },
            onOdometerChange = { odometer = it },
            onNotesChange = { notes = it },
            onPickImage = {
                imageTargetMotorcycleId = null
                imagePicker.launch(arrayOf("image/*"))
            },
            onRemoveImage = { index ->
                selectedImageUris = selectedImageUris.filterIndexed { i, _ -> i != index }
            },
            onPreviewImage = { uri -> previewImageUri = uri },
            onSave = {
                val ok = if (editId == null) {
                    vm.addMotorcycle(
                        name = name,
                        plateNumber = plateNumber,
                        vin = vin,
                        purchaseDateIso = purchaseDateIso,
                        odometerInput = odometer,
                        notes = notes,
                        imageUris = selectedImageUris
                    )
                } else {
                    vm.updateMotorcycle(
                        motorcycleId = editId!!,
                        name = name,
                        plateNumber = plateNumber,
                        vin = vin,
                        purchaseDateIso = purchaseDateIso,
                        odometerInput = odometer,
                        notes = notes,
                        imageUris = selectedImageUris
                    )
                }
                if (ok) resetForm()
            },
            onCancel = { resetForm() },
            error = uiState.error,
            message = uiState.message
        )
    }

    if (previewImageUri != null) {
        FullScreenImagePreviewDialog(
            imageUri = previewImageUri.orEmpty(),
            onDismiss = { previewImageUri = null }
        )
    }
}

@Composable
private fun MotorcycleFormDialog(
    editId: String?,
    name: String,
    plateNumber: String,
    vin: String,
    purchaseDateIso: String,
    odometer: String,
    notes: String,
    selectedImageUris: List<String>,
    onNameChange: (String) -> Unit,
    onPlateNumberChange: (String) -> Unit,
    onVinChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onOdometerChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    error: String?,
    message: String?
) {
    FormDialogShell(
        title = "${if (editId == null) "Add" else "Edit"} Motorcycle",
        confirmText = if (editId == null) "Add" else "Update",
        onConfirm = onSave,
        onDismiss = onCancel,
        error = error,
        message = message
    ) {
        MotorcycleProfileFormFields(
            name = name,
            plateNumber = plateNumber,
            vin = vin,
            purchaseDateIso = purchaseDateIso,
            odometer = odometer,
            notes = notes,
            onNameChange = onNameChange,
            onPlateNumberChange = onPlateNumberChange,
            onVinChange = onVinChange,
            onPurchaseDateChange = onPurchaseDateChange,
            onOdometerChange = onOdometerChange,
            onNotesChange = onNotesChange
        )

        if (selectedImageUris.isNotEmpty()) {
            Text("Photos (${selectedImageUris.size})", style = MaterialTheme.typography.bodyMedium)
            selectedImageUris.forEachIndexed { index, uri ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photo ${index + 1}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    CompactIconAction(
                        icon = Icons.Filled.Visibility,
                        contentDescription = "Preview motorcycle photo ${index + 1}",
                        onClick = { onPreviewImage(uri) }
                    )
                    CompactIconAction(
                        icon = Icons.Filled.HideImage,
                        contentDescription = "Remove motorcycle photo ${index + 1}",
                        onClick = { onRemoveImage(index) }
                    )
                }
            }
        }
        Button(onClick = onPickImage) {
            Text(if (selectedImageUris.isEmpty()) "Add Motorcycle Photo" else "Add Photo")
        }
    }
}

@Composable
private fun MotorcycleRowCard(
    bike: MotorcycleProfile,
    isActive: Boolean,
    cardPadding: Dp,
    rowSpacing: Dp,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFirstPicture: () -> Unit,
    onPickImage: () -> Unit,
    onPreviewImage: (String) -> Unit
) {
    var detailsExpanded by rememberSaveable(bike.id) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
            val primaryImage = bike.primaryImageUri
            if (primaryImage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { onPreviewImage(primaryImage) }
                ) {
                    AsyncImage(
                        model = primaryImage,
                        contentDescription = "${bike.name} photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clickable { onPickImage() }
                ) {
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                            Text("Tap to add photo", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            CardHeaderRow(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = bike.name,
                titleMaxLines = 2,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            text = if (isActive) "Active" else "Stored",
                            tone = if (isActive) StatusTone.Positive else StatusTone.Info,
                            icon = Icons.AutoMirrored.Filled.DirectionsBike
                        )
                        CompactIconAction(
                            icon = if (detailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (detailsExpanded) "Hide bike details" else "Show bike details",
                            onClick = { detailsExpanded = !detailsExpanded }
                        )
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactIconAction(
                    icon = Icons.Filled.CheckCircle,
                    contentDescription = "Set active motorcycle",
                    onClick = onSelect,
                    enabled = !isActive
                )
                CompactIconAction(
                    icon = Icons.Filled.Edit,
                    contentDescription = "Edit motorcycle",
                    onClick = onEdit
                )
                CompactIconAction(
                    icon = Icons.Filled.AddAPhoto,
                    contentDescription = "Add motorcycle photo",
                    onClick = onPickImage
                )
                if (bike.imageUris.isNotEmpty()) {
                    CompactIconAction(
                        icon = Icons.Filled.HideImage,
                        contentDescription = "Remove first motorcycle photo",
                        onClick = onRemoveFirstPicture
                    )
                }
                CompactIconAction(
                    icon = Icons.Filled.Delete,
                    contentDescription = "Delete motorcycle",
                    onClick = onDelete,
                    enabled = !isActive
                )
            }

            LabeledBikeLine(icon = Icons.Filled.Badge, text = "Plate: ${bike.plateNumber}")
            LabeledBikeLine(icon = Icons.Filled.Speed, text = "Odometer: ${bike.currentOdometerKm} km")
            if (detailsExpanded) {
                LabeledBikeLine(icon = Icons.Filled.VpnKey, text = "VIN: ${bike.vin}")
                LabeledBikeLine(icon = Icons.Filled.CalendarMonth, text = "Purchased: ${bike.purchaseDateIso}")
                if (bike.notes.isNotBlank()) {
                    Text(bike.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LabeledBikeLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text)
    }
}

@Composable
private fun FullScreenImagePreviewDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Motorcycle image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview")
                }
            }
        }
    }
}

@Preview(name = "Motorcycle Card - Active", showBackground = true)
@Composable
private fun MotorcycleRowCardPreview() {
    MyApplicationTheme {
        MotorcycleRowCard(
            bike = MotorcycleProfile(
                id = "bike-preview",
                name = "Weekend Bike",
                plateNumber = "ABC-4321",
                vin = "JYARN23E1PA654321",
                purchaseDateIso = "2024-03-18",
                currentOdometerKm = 12890,
                notes = "Set up for long rides",
                imageUris = emptyList()
            ),
            isActive = true,
            cardPadding = 16.dp,
            rowSpacing = 8.dp,
            onSelect = {},
            onEdit = {},
            onDelete = {},
            onRemoveFirstPicture = {},
            onPickImage = {},
            onPreviewImage = {}
        )
    }
}

@Preview(name = "Motorcycle Form - Add", showBackground = true)
@Composable
private fun MotorcycleFormDialogPreview() {
    MyApplicationTheme {
        MotorcycleFormDialog(
            editId = null,
            name = "Daily Ride",
            plateNumber = "ABC-1234",
            vin = "JYARN23E1PA123456",
            purchaseDateIso = "2025-01-10",
            odometer = "15420",
            notes = "Main city bike",
            selectedImageUris = emptyList(),
            onNameChange = {},
            onPlateNumberChange = {},
            onVinChange = {},
            onPurchaseDateChange = {},
            onOdometerChange = {},
            onNotesChange = {},
            onPickImage = {},
            onRemoveImage = {},
            onPreviewImage = {},
            onSave = {},
            onCancel = {},
            error = null,
            message = null
        )
    }
}
