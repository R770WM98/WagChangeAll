package com.thisismine.myapplication.feature.maintenance

import com.thisismine.myapplication.feature.maintenance.data.MaintenanceRepository
import com.thisismine.myapplication.feature.maintenance.model.PartRecord
import com.thisismine.myapplication.feature.maintenance.model.ServiceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceViewModelTest {

    @Test
    fun serviceFilter_filtersByType() {
        val repository = FakeMaintenanceRepository()
        val viewModel = MaintenanceViewModel(repository)

        viewModel.onServiceFilterQueryChange("oil")

        val filtered = viewModel.uiState.value.filteredServices
        assertEquals(1, filtered.size)
        assertTrue(filtered.first().serviceType.contains("Oil", ignoreCase = true))
    }

    @Test
    fun partFilter_filtersByName() {
        val repository = FakeMaintenanceRepository()
        val viewModel = MaintenanceViewModel(repository)

        viewModel.onPartFilterQueryChange("pad")

        val filtered = viewModel.uiState.value.filteredParts
        assertEquals(1, filtered.size)
        assertTrue(filtered.first().partName.contains("Pad", ignoreCase = true))
    }

    @Test
    fun requestCancelDeleteService_keepsData() {
        val repository = FakeMaintenanceRepository()
        val viewModel = MaintenanceViewModel(repository)
        val serviceId = viewModel.uiState.value.services.first().id

        viewModel.requestDeleteService(serviceId)
        assertEquals(serviceId, viewModel.uiState.value.pendingDeleteServiceId)

        viewModel.cancelDeleteService()
        assertEquals(null, viewModel.uiState.value.pendingDeleteServiceId)
        assertTrue(viewModel.uiState.value.services.any { it.id == serviceId })
    }

    @Test
    fun confirmDeletePart_removesPartRecord() {
        val repository = FakeMaintenanceRepository()
        val viewModel = MaintenanceViewModel(repository)
        val partId = viewModel.uiState.value.parts.first().id

        viewModel.requestDeletePart(partId)
        viewModel.confirmDeletePart()

        assertFalse(viewModel.uiState.value.parts.any { it.id == partId })
    }

    @Test
    fun refresh_exposesServiceTemplates() {
        val viewModel = MaintenanceViewModel(FakeMaintenanceRepository())
        assertTrue(viewModel.uiState.value.serviceTemplates.isNotEmpty())
        assertTrue(viewModel.uiState.value.serviceTemplates.any { it.contains("Oil", ignoreCase = true) })
    }

    @Test
    fun savePart_acceptsIntervalAndWarranty() {
        val repository = FakeMaintenanceRepository()
        val viewModel = MaintenanceViewModel(repository)

        val ok = viewModel.savePart(
            editingId = null,
            partName = "Drive Belt",
            dateIso = "2026-04-07",
            odometerInput = "13000",
            costInput = "1850",
            notes = "Installed with rollers",
            costLevelOverride = null,
            replacementIntervalKmInput = "12000",
            warrantyExpiryIsoInput = "2027-04-07",
            imageUris = emptyList()
        )

        assertTrue(ok)
        assertTrue(viewModel.uiState.value.parts.any { it.partName == "Drive Belt" && it.replacementIntervalKm == 12000 })
    }
}

private class FakeMaintenanceRepository : MaintenanceRepository {
    private val services = mutableListOf(
        ServiceRecord(
            id = "s1",
            dateIso = "2026-03-10",
            displayDate = "Mar 10, 2026",
            odometerKm = 12000,
            serviceType = "Engine Oil Change",
            totalCostPhp = 700.0,
            notes = "Regular service"
        ),
        ServiceRecord(
            id = "s2",
            dateIso = "2026-03-20",
            displayDate = "Mar 20, 2026",
            odometerKm = 12300,
            serviceType = "CVT Cleaning",
            totalCostPhp = 500.0,
            notes = "Routine"
        )
    )

    private val parts = mutableListOf(
        PartRecord(
            id = "p1",
            dateIso = "2026-03-11",
            displayDate = "Mar 11, 2026",
            odometerKm = 12100,
            partName = "Brake Pad",
            totalCostPhp = 450.0,
            notes = "Front"
        ),
        PartRecord(
            id = "p2",
            dateIso = "2026-03-22",
            displayDate = "Mar 22, 2026",
            odometerKm = 12400,
            partName = "Spark Plug",
            totalCostPhp = 180.0,
            notes = "Replacement"
        )
    )

    override fun getCurrentOdometerKm(): Int = 12400

    override fun getServiceRecords(): List<ServiceRecord> = services.toList()

    override fun getPartRecords(): List<PartRecord> = parts.toList()

    override fun getServiceTemplates(): List<String> = listOf(
        "Engine Oil Change",
        "Brake Fluid Flush",
        "CVT Cleaning",
        "Tire Rotation / Check"
    )

    override fun addServiceRecord(
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        services.add(
            ServiceRecord(
                id = "s-${services.size + 1}",
                dateIso = dateIso,
                displayDate = dateIso,
                odometerKm = odometerKm,
                serviceType = serviceType,
                totalCostPhp = totalCostPhp,
                notes = notes
            )
        )
    }

    override fun updateServiceRecord(
        id: String,
        serviceType: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        imageUris: List<String>
    ) {
        val index = services.indexOfFirst { it.id == id }
        if (index >= 0) {
            services[index] = services[index].copy(
                serviceType = serviceType,
                dateIso = dateIso,
                displayDate = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes
            )
        }
    }

    override fun deleteServiceRecord(id: String) {
        services.removeAll { it.id == id }
    }

    override fun addPartRecord(
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        replacementIntervalKm: Int?,
        warrantyExpiryIso: String?,
        imageUris: List<String>
    ) {
        parts.add(
            PartRecord(
                id = "p-${parts.size + 1}",
                dateIso = dateIso,
                displayDate = dateIso,
                odometerKm = odometerKm,
                partName = partName,
                totalCostPhp = totalCostPhp,
                notes = notes,
                replacementIntervalKm = replacementIntervalKm,
                warrantyExpiryIso = warrantyExpiryIso
            )
        )
    }

    override fun updatePartRecord(
        id: String,
        partName: String,
        dateIso: String,
        odometerKm: Int,
        totalCostPhp: Double,
        notes: String,
        costLevelOverride: String?,
        replacementIntervalKm: Int?,
        warrantyExpiryIso: String?,
        imageUris: List<String>
    ) {
        val index = parts.indexOfFirst { it.id == id }
        if (index >= 0) {
            parts[index] = parts[index].copy(
                partName = partName,
                dateIso = dateIso,
                displayDate = dateIso,
                odometerKm = odometerKm,
                totalCostPhp = totalCostPhp,
                notes = notes,
                replacementIntervalKm = replacementIntervalKm,
                warrantyExpiryIso = warrantyExpiryIso
            )
        }
    }

    override fun deletePartRecord(id: String) {
        parts.removeAll { it.id == id }
    }
}
