@file:OptIn(ExperimentalLayoutApi::class)

package com.thisismine.myapplication.feature.maintenance

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thisismine.myapplication.feature.maintenance.model.PartRecord
import com.thisismine.myapplication.feature.maintenance.model.ServiceRecord
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import com.thisismine.myapplication.core.ui.CardHeaderRow
import com.thisismine.myapplication.core.ui.CompactIconAction
import com.thisismine.myapplication.core.ui.CostRow
import com.thisismine.myapplication.core.ui.LocalCardDensity
import com.thisismine.myapplication.core.ui.StatusChip
import com.thisismine.myapplication.core.ui.StatusTone
import com.thisismine.myapplication.core.ui.DatePickerField
import java.time.LocalDate
import androidx.compose.ui.tooling.preview.Preview
import com.thisismine.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.thisismine.myapplication.data.repository.WagChangeRepository

data class ServiceDraft(
    val editingId: String? = null,
    val serviceType: String = "",
    val dateIso: String = "",
    val odometer: String = "",
    val cost: String = "",
    val notes: String = "",
    val costLevelSelection: String = "AUTO",
    val imageUris: List<String> = emptyList()
)

data class PartDraft(
    val editingId: String? = null,
    val partName: String = "",
    val dateIso: String = "",
    val odometer: String = "",
    val cost: String = "",
    val notes: String = "",
    val costLevelSelection: String = "AUTO",
    val replacementIntervalKm: String = "",
    val warrantyExpiryIso: String = "",
    val imageUris: List<String> = emptyList()
)

private enum class MaintenancePhotoTarget {
    Service,
    Part
}

@Composable
fun MaintenanceScreen(
    vm: MaintenanceViewModel,
    wagChangeRepository: WagChangeRepository
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val activeMotorcycle by wagChangeRepository.activeMotorcycleFlow.collectAsStateWithLifecycle()
    val pendingServiceDelete = uiState.services.firstOrNull { it.id == uiState.pendingDeleteServiceId }
    val pendingPartDelete = uiState.parts.firstOrNull { it.id == uiState.pendingDeletePartId }

    var serviceDraft by remember { mutableStateOf(ServiceDraft()) }
    var partDraft by remember { mutableStateOf(PartDraft()) }
    var showServiceDialog by remember { mutableStateOf(false) }
    var showPartDialog by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var pendingPhotoTarget by remember { mutableStateOf<MaintenancePhotoTarget?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = pendingPhotoTarget
        pendingPhotoTarget = null
        if (uri == null || target == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        val imageUri = uri.toString()
        when (target) {
            MaintenancePhotoTarget.Service -> {
                if (!serviceDraft.imageUris.contains(imageUri)) {
                    serviceDraft = serviceDraft.copy(imageUris = serviceDraft.imageUris + imageUri)
                }
            }

            MaintenancePhotoTarget.Part -> {
                if (!partDraft.imageUris.contains(imageUri)) {
                    partDraft = partDraft.copy(imageUris = partDraft.imageUris + imageUri)
                }
            }
        }
    }

    LaunchedEffect(activeMotorcycle?.id) {
        vm.refresh()
    }

    fun resetServiceDraft() {
        serviceDraft = ServiceDraft()
        showServiceDialog = false
    }

    fun resetPartDraft() {
        partDraft = PartDraft()
        showPartDialog = false
    }

    fun defaultDateIso(): String = LocalDate.now().toString()

    val density = LocalCardDensity.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(density.listContentPadding),
        verticalArrangement = Arrangement.spacedBy(density.sectionSpacing)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Service Records", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        serviceDraft = ServiceDraft(
                            dateIso = defaultDateIso(),
                            odometer = uiState.currentOdometerKm.toString()
                        )
                        showServiceDialog = true
                    }
                ) { Text("Add Service") }
            }
        }
        item {
            OutlinedTextField(
                value = uiState.serviceFilterQuery,
                onValueChange = vm::onServiceFilterQueryChange,
                label = { Text("Filter services") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (uiState.filteredServices.isEmpty()) {
            item { Text("No maintenance records yet.") }
        } else {
            items(uiState.filteredServices, key = { it.id }) { record ->
                ServiceRecordCard(
                    record = record,
                    cardPadding = density.cardPadding,
                    onEdit = {
                        serviceDraft = ServiceDraft(
                            editingId = record.id,
                            serviceType = record.serviceType,
                            dateIso = record.dateIso,
                            odometer = record.odometerKm.toString(),
                            cost = record.totalCostPhp.toString(),
                            notes = record.notes,
                            costLevelSelection = normalizeCostLevelUiSelection(record.costLevelOverride),
                            imageUris = record.imageUris
                        )
                        showServiceDialog = true
                    },
                    onDelete = { vm.requestDeleteService(record.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Part Replacements", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        partDraft = PartDraft(
                            dateIso = defaultDateIso(),
                            odometer = uiState.currentOdometerKm.toString()
                        )
                        showPartDialog = true
                    }
                ) { Text("Add Part") }
            }
        }
        item {
            OutlinedTextField(
                value = uiState.partFilterQuery,
                onValueChange = vm::onPartFilterQueryChange,
                label = { Text("Filter parts") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (uiState.filteredParts.isEmpty()) {
            item { Text("No part replacements yet.") }
        } else {
            items(uiState.filteredParts, key = { it.id }) { record ->
                PartRecordCard(
                    record = record,
                    cardPadding = density.cardPadding,
                    onEdit = {
                        partDraft = PartDraft(
                            editingId = record.id,
                            partName = record.partName,
                            dateIso = record.dateIso,
                            odometer = record.odometerKm.toString(),
                            cost = record.totalCostPhp.toString(),
                            notes = record.notes,
                            costLevelSelection = normalizeCostLevelUiSelection(record.costLevelOverride),
                            replacementIntervalKm = record.replacementIntervalKm?.toString().orEmpty(),
                            warrantyExpiryIso = record.warrantyExpiryIso.orEmpty(),
                            imageUris = record.imageUris
                        )
                        showPartDialog = true
                    },
                    onDelete = { vm.requestDeletePart(record.id) }
                )
            }
        }
    }

    if (showServiceDialog) {
        ServiceFormDialog(
            draft = serviceDraft,
            onDraftChange = { serviceDraft = it },
            templates = uiState.serviceTemplates,
            onSave = {
                val ok = vm.saveService(
                    editingId = serviceDraft.editingId,
                    serviceType = serviceDraft.serviceType,
                    dateIso = serviceDraft.dateIso,
                    odometerInput = serviceDraft.odometer,
                    costInput = serviceDraft.cost,
                    notes = serviceDraft.notes,
                    costLevelOverride = normalizeCostLevelSelection(serviceDraft.costLevelSelection),
                    imageUris = serviceDraft.imageUris
                )
                if (ok) resetServiceDraft()
            },
            onCancelEdit = { resetServiceDraft() },
            onPickImage = {
                pendingPhotoTarget = MaintenancePhotoTarget.Service
                imagePicker.launch(arrayOf("image/*"))
            },
            onRemoveImage = { index ->
                serviceDraft = serviceDraft.copy(
                    imageUris = serviceDraft.imageUris.filterIndexed { i, _ -> i != index }
                )
            },
            onPreviewImage = { uri -> previewImageUri = uri },
            error = uiState.error,
            message = uiState.message
        )
    }

    if (showPartDialog) {
        PartFormDialog(
            draft = partDraft,
            onDraftChange = { partDraft = it },
            onSave = {
                val ok = vm.savePart(
                    editingId = partDraft.editingId,
                    partName = partDraft.partName,
                    dateIso = partDraft.dateIso,
                    odometerInput = partDraft.odometer,
                    costInput = partDraft.cost,
                    notes = partDraft.notes,
                    costLevelOverride = normalizeCostLevelSelection(partDraft.costLevelSelection),
                    replacementIntervalKmInput = partDraft.replacementIntervalKm,
                    warrantyExpiryIsoInput = partDraft.warrantyExpiryIso,
                    imageUris = partDraft.imageUris
                )
                if (ok) resetPartDraft()
            },
            onCancelEdit = { resetPartDraft() },
            onPickImage = {
                pendingPhotoTarget = MaintenancePhotoTarget.Part
                imagePicker.launch(arrayOf("image/*"))
            },
            onRemoveImage = { index ->
                partDraft = partDraft.copy(
                    imageUris = partDraft.imageUris.filterIndexed { i, _ -> i != index }
                )
            },
            onPreviewImage = { uri -> previewImageUri = uri },
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

    if (pendingServiceDelete != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDeleteService,
            title = { Text("Delete service record?") },
            text = { Text("Delete ${pendingServiceDelete.serviceType} (${pendingServiceDelete.displayDate})?") },
            confirmButton = { TextButton(onClick = vm::confirmDeleteService) { Text("Delete") } },
            dismissButton = { TextButton(onClick = vm::cancelDeleteService) { Text("Cancel") } }
        )
    }

    if (pendingPartDelete != null) {
        AlertDialog(
            onDismissRequest = vm::cancelDeletePart,
            title = { Text("Delete part record?") },
            text = { Text("Delete ${pendingPartDelete.partName} (${pendingPartDelete.displayDate})?") },
            confirmButton = { TextButton(onClick = vm::confirmDeletePart) { Text("Delete") } },
            dismissButton = { TextButton(onClick = vm::cancelDeletePart) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceFormDialog(
    draft: ServiceDraft,
    onDraftChange: (ServiceDraft) -> Unit,
    templates: List<String>,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    error: String?,
    message: String?
) {
    BasicAlertDialog(onDismissRequest = onCancelEdit, modifier = Modifier.fillMaxWidth(0.9f)) {
        ServiceFormCard(
            draft = draft,
            onDraftChange = onDraftChange,
            templates = templates,
            onSave = onSave,
            onCancelEdit = onCancelEdit,
            onPickImage = onPickImage,
            onRemoveImage = onRemoveImage,
            onPreviewImage = onPreviewImage,
            error = error,
            message = message
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartFormDialog(
    draft: PartDraft,
    onDraftChange: (PartDraft) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    error: String?,
    message: String?
) {
    BasicAlertDialog(onDismissRequest = onCancelEdit, modifier = Modifier.fillMaxWidth(0.9f)) {
        PartFormCard(
            draft = draft,
            onDraftChange = onDraftChange,
            onSave = onSave,
            onCancelEdit = onCancelEdit,
            onPickImage = onPickImage,
            onRemoveImage = onRemoveImage,
            onPreviewImage = onPreviewImage,
            error = error,
            message = message
        )
    }
}

@Composable
private fun ServiceFormCard(
    draft: ServiceDraft,
    onDraftChange: (ServiceDraft) -> Unit,
    templates: List<String>,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    error: String?,
    message: String?
) {
    val density = LocalCardDensity.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(density.cardPadding),
            verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
            ) {
                Text(
                    text = if (draft.editingId == null) "Add Service" else "Edit Service",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = draft.serviceType,
                    onValueChange = { onDraftChange(draft.copy(serviceType = it)) },
                    label = { Text("Service type") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (templates.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { template ->
                            FilterChip(
                                selected = draft.serviceType == template,
                                onClick = { onDraftChange(draft.copy(serviceType = template)) },
                                label = { Text(template) }
                            )
                        }
                    }
                }
                DatePickerField(
                    value = draft.dateIso,
                    onValueChange = { onDraftChange(draft.copy(dateIso = it)) },
                    label = "Date",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.odometer,
                    onValueChange = { onDraftChange(draft.copy(odometer = it)) },
                    label = { Text("Odometer km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.cost,
                    onValueChange = { onDraftChange(draft.copy(cost = it)) },
                    label = { Text("Cost PHP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CostLevelPicker(
                    selected = draft.costLevelSelection,
                    onSelectedChange = { onDraftChange(draft.copy(costLevelSelection = it)) }
                )
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onDraftChange(draft.copy(notes = it)) },
                    label = { Text("Notes") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                ImageAttachmentsSection(
                    imageUris = draft.imageUris,
                    onPickImage = onPickImage,
                    onRemoveImage = onRemoveImage,
                    onPreviewImage = onPreviewImage,
                    addButtonLabel = if (draft.imageUris.isEmpty()) "Add Service Photo" else "Add Photo"
                )

                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(density.rowSpacing)) {
                    Button(onClick = onSave) {
                        Text(if (draft.editingId == null) "Add" else "Update")
                    }
                    Button(onClick = onCancelEdit) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun PartFormCard(
    draft: PartDraft,
    onDraftChange: (PartDraft) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    error: String?,
    message: String?
) {
    val density = LocalCardDensity.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(density.cardPadding),
            verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
            ) {
                Text(
                    text = if (draft.editingId == null) "Add Part" else "Edit Part",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = draft.partName,
                    onValueChange = { onDraftChange(draft.copy(partName = it)) },
                    label = { Text("Part name") },
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickerField(
                    value = draft.dateIso,
                    onValueChange = { onDraftChange(draft.copy(dateIso = it)) },
                    label = "Date",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.odometer,
                    onValueChange = { onDraftChange(draft.copy(odometer = it)) },
                    label = { Text("Odometer km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.cost,
                    onValueChange = { onDraftChange(draft.copy(cost = it)) },
                    label = { Text("Cost PHP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CostLevelPicker(
                    selected = draft.costLevelSelection,
                    onSelectedChange = { onDraftChange(draft.copy(costLevelSelection = it)) }
                )
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onDraftChange(draft.copy(notes = it)) },
                    label = { Text("Notes") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.replacementIntervalKm,
                    onValueChange = { onDraftChange(draft.copy(replacementIntervalKm = it)) },
                    label = { Text("Replace interval km (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickerField(
                    value = draft.warrantyExpiryIso,
                    onValueChange = { onDraftChange(draft.copy(warrantyExpiryIso = it)) },
                    label = "Warranty expiry (optional)",
                    modifier = Modifier.fillMaxWidth(),
                    allowClear = true
                )

                ImageAttachmentsSection(
                    imageUris = draft.imageUris,
                    onPickImage = onPickImage,
                    onRemoveImage = onRemoveImage,
                    onPreviewImage = onPreviewImage,
                    addButtonLabel = if (draft.imageUris.isEmpty()) "Add Part Photo" else "Add Photo"
                )

                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(density.rowSpacing)) {
                    Button(onClick = onSave) {
                        Text(if (draft.editingId == null) "Add" else "Update")
                    }
                    Button(onClick = onCancelEdit) { Text("Cancel") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostLevelPicker(
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    val normalizedSelected = normalizeCostLevelUiSelection(selected)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Cost level", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("AUTO", "LOW", "MEDIUM", "HIGH").forEach { option ->
                FilterChip(
                    selected = normalizedSelected == option,
                    onClick = { onSelectedChange(option) },
                    label = {
                        Text(
                            when (option) {
                                "AUTO" -> "Auto"
                                "LOW" -> "Low"
                                "MEDIUM" -> "Medium"
                                else -> "High"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ServiceRecordCard(
    record: ServiceRecord,
    cardPadding: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalCardDensity.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
        ) {
            CardHeaderRow(
                icon = Icons.Filled.Build,
                title = record.serviceType,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            text = costLabel(record.totalCostPhp, record.costLevelOverride),
                            tone = costTone(record.totalCostPhp, record.costLevelOverride)
                        )
                        CompactIconAction(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Edit service",
                            onClick = onEdit
                        )
                        CompactIconAction(
                            icon = Icons.Filled.Delete,
                            contentDescription = "Delete service",
                            onClick = onDelete
                        )
                    }
                }
            )
            MaintenanceMetaRows(
                displayDate = record.displayDate,
                odometerKm = record.odometerKm,
                costPhp = record.totalCostPhp
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.imageUris.isNotEmpty()) {
                Text("Photos: ${record.imageUris.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PartRecordCard(
    record: PartRecord,
    cardPadding: Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalCardDensity.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(density.rowSpacing)
        ) {
            CardHeaderRow(
                icon = Icons.Filled.Inventory2,
                title = record.partName,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            text = costLabel(record.totalCostPhp, record.costLevelOverride),
                            tone = costTone(record.totalCostPhp, record.costLevelOverride)
                        )
                        CompactIconAction(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Edit part",
                            onClick = onEdit
                        )
                        CompactIconAction(
                            icon = Icons.Filled.Delete,
                            contentDescription = "Delete part",
                            onClick = onDelete
                        )
                    }
                }
            )
            MaintenanceMetaRows(
                displayDate = record.displayDate,
                odometerKm = record.odometerKm,
                costPhp = record.totalCostPhp
            )
            if (record.remainingKmEstimate != null) {
                val remaining = record.remainingKmEstimate
                Text(
                    text = if (remaining < 0) "Estimated life: overdue by ${-remaining} km" else "Estimated life remaining: $remaining km",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!record.warrantyStatusText.isNullOrBlank()) {
                Text(record.warrantyStatusText, style = MaterialTheme.typography.bodySmall)
            }
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.imageUris.isNotEmpty()) {
                Text("Photos: ${record.imageUris.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ImageAttachmentsSection(
    imageUris: List<String>,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPreviewImage: (String) -> Unit,
    addButtonLabel: String
) {
    if (imageUris.isNotEmpty()) {
        Text("Photos (${imageUris.size})", style = MaterialTheme.typography.bodyMedium)
        imageUris.forEachIndexed { index, uri ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Photo ${index + 1}",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                CompactIconAction(
                    icon = Icons.Filled.Visibility,
                    contentDescription = "Preview photo ${index + 1}",
                    onClick = { onPreviewImage(uri) }
                )
                CompactIconAction(
                    icon = Icons.Filled.HideImage,
                    contentDescription = "Remove photo ${index + 1}",
                    onClick = { onRemoveImage(index) }
                )
            }
        }
    }

    Button(onClick = onPickImage) {
        Icon(Icons.Filled.AddAPhoto, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(addButtonLabel)
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
                    contentDescription = "Maintenance image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview")
                }
            }
        }
    }
}

@Composable
private fun MaintenanceMetaRows(
    displayDate: String,
    odometerKm: Int,
    costPhp: Double
) {
    Text("Date: $displayDate")
    Text("Odometer: $odometerKm km")
    CostRow(amount = costPhp)
}

@Composable
private fun MaintenanceCardActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Keep for backward compatibility with previews and future reuse.
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onEdit) { Text("Edit") }
        Button(onClick = onDelete) { Text("Delete") }
    }
}

private fun costTone(cost: Double, override: String?): StatusTone = when (effectiveLevel(cost, override)) {
    "HIGH" -> StatusTone.Critical
    "MEDIUM" -> StatusTone.Warning
    else -> StatusTone.Info
}

private fun costLabel(cost: Double, override: String?): String = when (effectiveLevel(cost, override)) {
    "HIGH" -> "High"
    "MEDIUM" -> "Medium"
    else -> "Low"
}

private fun effectiveLevel(cost: Double, override: String?): String {
    val normalized = override?.trim()?.uppercase()
    if (normalized == "LOW" || normalized == "MEDIUM" || normalized == "HIGH") return normalized
    return when {
        cost >= 2000.0 -> "HIGH"
        cost >= 800.0 -> "MEDIUM"
        else -> "LOW"
    }
}

private fun normalizeCostLevelSelection(selection: String): String? = when (selection.trim().uppercase()) {
    "LOW", "MEDIUM", "HIGH" -> selection.trim().uppercase()
    else -> null
}

private fun normalizeCostLevelUiSelection(raw: String?): String {
    val normalized = raw?.trim()?.uppercase()
    return when (normalized) {
        "LOW", "MEDIUM", "HIGH" -> normalized
        else -> "AUTO"
    }
}

@Composable
private fun CostLevelPickerDuplicate(
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    val normalizedSelected = normalizeCostLevelUiSelection(selected)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Cost level", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("AUTO", "LOW", "MEDIUM", "HIGH").forEach { option ->
                FilterChip(
                    selected = normalizedSelected == option,
                    onClick = { onSelectedChange(option) },
                    label = {
                        Text(
                            when (option) {
                                "AUTO" -> "Auto"
                                "LOW" -> "Low"
                                "MEDIUM" -> "Medium"
                                else -> "High"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Preview(name = "Service Record Card", showBackground = true)
@Composable
private fun ServiceRecordCardPreview() {
    MyApplicationTheme {
        ServiceRecordCard(
            record = ServiceRecord(
                id = "srv-preview",
                dateIso = "2026-04-01",
                displayDate = "Apr 01, 2026",
                odometerKm = 21450,
                serviceType = "Engine Oil Change",
                totalCostPhp = 780.0,
                notes = "Used 10W-40 and replaced drain washer."
            ),
            cardPadding = 16.dp,
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Part Record Card", showBackground = true)
@Composable
private fun PartRecordCardPreview() {
    MyApplicationTheme {
        PartRecordCard(
            record = PartRecord(
                id = "part-preview",
                dateIso = "2026-03-20",
                displayDate = "Mar 20, 2026",
                odometerKm = 20800,
                partName = "Rear Tire",
                totalCostPhp = 2450.0,
                notes = "Switched to touring compound."
            ),
            cardPadding = 16.dp,
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Service Form", showBackground = true)
@Composable
private fun ServiceFormCardPreview() {
    MyApplicationTheme {
        ServiceFormCard(
            draft = ServiceDraft(
                serviceType = "CVT Cleaning",
                dateIso = "2026-04-04",
                odometer = "22000",
                cost = "650",
                notes = "Checked roller weights and clutch bell."
            ),
            onDraftChange = {},
            templates = listOf("Engine Oil Change", "CVT Cleaning"),
            onSave = {},
            onCancelEdit = {},
            onPickImage = {},
            onRemoveImage = {},
            onPreviewImage = {},
            error = null,
            message = null
        )
    }
}

@Preview(name = "Part Form", showBackground = true)
@Composable
private fun PartFormCardPreview() {
    MyApplicationTheme {
        PartFormCard(
            draft = PartDraft(
                partName = "Brake Pads",
                dateIso = "2026-04-04",
                odometer = "22000",
                cost = "850",
                notes = "Front pads replaced.",
                replacementIntervalKm = "6000",
                warrantyExpiryIso = "2026-12-31"
            ),
            onDraftChange = {},
            onSave = {},
            onCancelEdit = {},
            onPickImage = {},
            onRemoveImage = {},
            onPreviewImage = {},
            error = null,
            message = null
        )
    }
}
