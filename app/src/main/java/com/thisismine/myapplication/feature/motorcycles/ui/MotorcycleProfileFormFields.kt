package com.thisismine.myapplication.feature.motorcycles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thisismine.myapplication.core.ui.DatePickerField
import com.thisismine.myapplication.ui.theme.MyApplicationTheme

@Composable
fun MotorcycleProfileFormFields(
    name: String,
    plateNumber: String,
    vin: String,
    purchaseDateIso: String,
    odometer: String,
    notes: String,
    onNameChange: (String) -> Unit,
    onPlateNumberChange: (String) -> Unit,
    onVinChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onOdometerChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(name, onNameChange, label = { Text("Motorcycle model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(plateNumber, onPlateNumberChange, label = { Text("Plate Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(vin, onVinChange, label = { Text("Motorcycle Chassis Number") }, modifier = Modifier.fillMaxWidth())
        DatePickerField(
            value = purchaseDateIso,
            onValueChange = onPurchaseDateChange,
            label = "Purchase date",
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            odometer,
            onOdometerChange,
            label = { Text("Current odometer km") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(notes, onNotesChange, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Motorcycle Profile Fields", showBackground = true)
@Composable
private fun MotorcycleProfileFormFieldsPreview() {
    MyApplicationTheme {
        MotorcycleProfileFormFields(
            name = "Daily Ride",
            plateNumber = "ABC-1234",
            vin = "JYARN23E1PA123456",
            purchaseDateIso = "2025-05-10",
            odometer = "15420",
            notes = "Main city bike",
            onNameChange = {},
            onPlateNumberChange = {},
            onVinChange = {},
            onPurchaseDateChange = {},
            onOdometerChange = {},
            onNotesChange = {}
        )
    }
}
