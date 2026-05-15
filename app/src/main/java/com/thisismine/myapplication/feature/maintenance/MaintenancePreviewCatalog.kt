package com.thisismine.myapplication.feature.maintenance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.thisismine.myapplication.data.repository.InMemoryWagChangeRepository
import com.thisismine.myapplication.ui.theme.MyApplicationTheme

@Preview(name = "Maintenance Catalog", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun MaintenanceCatalogPreview() {
    val repository = remember { InMemoryWagChangeRepository() }
    MyApplicationTheme {
        MaintenanceLogScreen(
            wagChangeRepository = repository
        )
    }
}
