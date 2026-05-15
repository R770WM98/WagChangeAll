package com.thisismine.myapplication.feature.motorcycles.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thisismine.myapplication.core.ui.FormDialogShell

@Composable
fun MotorcycleProfileDialogShell(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    confirmEnabled: Boolean = true,
    error: String? = null,
    message: String? = null,
    content: @Composable () -> Unit
) {
    FormDialogShell(
        title = title,
        confirmText = confirmText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        dismissText = dismissText,
        confirmEnabled = confirmEnabled,
        error = error,
        message = message,
        content = content
    )
}
