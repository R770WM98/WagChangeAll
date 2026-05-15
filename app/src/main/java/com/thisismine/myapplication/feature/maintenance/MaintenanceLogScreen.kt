package com.thisismine.myapplication.feature.maintenance

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisismine.myapplication.data.repository.WagChangeRepository
import com.thisismine.myapplication.feature.maintenance.data.WagChangeMaintenanceRepository

@Composable
fun MaintenanceLogScreen(
    wagChangeRepository: WagChangeRepository
) {
    val vm: MaintenanceViewModel = viewModel(
        factory = MaintenanceViewModelFactory(
            repository = WagChangeMaintenanceRepository(wagChangeRepository)
        )
    )

    MaintenanceScreen(vm = vm)
}
