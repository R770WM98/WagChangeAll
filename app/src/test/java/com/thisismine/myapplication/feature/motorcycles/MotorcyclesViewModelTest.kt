package com.thisismine.myapplication.feature.motorcycles

import com.thisismine.myapplication.data.repository.InMemoryWagChangeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorcyclesViewModelTest {

    @Test
    fun filterQuery_filtersMotorcycleList() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        viewModel.addMotorcycle("Weekend Ride", "ABC-2222", "VIN-001", "2024-01-10", "1200", "", emptyList())
        viewModel.addMotorcycle("Track Bike", "ABC-3333", "VIN-002", "2023-03-15", "2000", "", emptyList())

        viewModel.onFilterQueryChange("abc-333")

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredMotorcycles.size)
        assertEquals("Track Bike", state.filteredMotorcycles.first().name)
    }

    @Test
    fun requestCancelDelete_doesNotRemoveMotorcycle() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        viewModel.addMotorcycle("Weekend Ride", "ABC-2222", "VIN-001", "2024-01-10", "1200", "", emptyList())
        val targetId = viewModel.uiState.value.motorcycles.first { it.name == "Weekend Ride" }.id

        viewModel.requestDeleteMotorcycle(targetId)
        assertEquals(targetId, viewModel.uiState.value.pendingDeleteMotorcycleId)

        viewModel.cancelDeleteMotorcycle()
        assertEquals(null, viewModel.uiState.value.pendingDeleteMotorcycleId)
        assertTrue(viewModel.uiState.value.motorcycles.any { it.id == targetId })
    }

    @Test
    fun confirmDelete_removesPendingMotorcycle() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        viewModel.addMotorcycle("Weekend Ride", "ABC-2222", "VIN-001", "2024-01-10", "1200", "", emptyList())
        val targetId = viewModel.uiState.value.motorcycles.first { it.name == "Weekend Ride" }.id

        viewModel.requestDeleteMotorcycle(targetId)
        viewModel.confirmDeleteMotorcycle()

        val state = viewModel.uiState.value
        assertFalse(state.motorcycles.any { it.id == targetId })
        assertNotNull(state.message)
        assertTrue(state.message!!.contains("deleted", ignoreCase = true))
    }

    @Test
    fun addMotorcycle_setsItAsActive() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        viewModel.addMotorcycle("Touring Bike", "ABC-4444", "VIN-003", "2022-09-01", "3000", "Long trips", emptyList())

        val state = viewModel.uiState.value
        val added = state.motorcycles.first { it.name == "Touring Bike" }
        assertEquals(added.id, state.activeMotorcycleId)
    }

    @Test
    fun activeMotorcycleFlow_tracksSelectionAndCreation() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        assertEquals("Daily Ride", repository.activeMotorcycleFlow.value?.name)

        viewModel.addMotorcycle("Touring Bike", "ABC-4444", "VIN-003", "2022-09-01", "3000", "Long trips", emptyList())
        assertEquals("Touring Bike", repository.activeMotorcycleFlow.value?.name)

        val dailyId = viewModel.uiState.value.motorcycles.first { it.name == "Daily Ride" }.id
        viewModel.selectMotorcycle(dailyId)
        assertEquals("Daily Ride", repository.activeMotorcycleFlow.value?.name)
    }

    @Test
    fun addMotorcycle_trimsProfileInput() {
        val repository = InMemoryWagChangeRepository()
        val viewModel = MotorcyclesViewModel(repository)

        val added = viewModel.addMotorcycle(
            name = "  Touring Bike  ",
            plateNumber = "  ABC-4444  ",
            vin = "  VIN-003  ",
            purchaseDateIso = " 2022-09-01 ",
            odometerInput = " 3000 ",
            notes = "  Long trips  ",
            imageUris = emptyList()
        )

        assertTrue(added)
        val bike = viewModel.uiState.value.motorcycles.first { it.name == "Touring Bike" }
        assertEquals("ABC-4444", bike.plateNumber)
        assertEquals("VIN-003", bike.vin)
        assertEquals("2022-09-01", bike.purchaseDateIso)
        assertEquals("Long trips", bike.notes)
    }
}
