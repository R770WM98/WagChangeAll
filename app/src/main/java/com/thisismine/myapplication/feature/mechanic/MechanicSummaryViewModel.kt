package com.thisismine.myapplication.feature.mechanic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thisismine.myapplication.ai.AiRepository
import com.thisismine.myapplication.ai.MechanicSummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MechanicSummaryUiState(
    val isLoading: Boolean = false,
    val summary: MechanicSummaryResponse? = null,
    val error: String? = null,
    val isAiEnabled: Boolean = false
)

class MechanicSummaryViewModel(
    private val aiRepository: AiRepository,
    private val activeBikeId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(MechanicSummaryUiState())
    val uiState = _uiState.asStateFlow()

    fun generateSummary(forceRefresh: Boolean = false) {
        val bikeId = activeBikeId ?: run {
            _uiState.value = _uiState.value.copy(
                error = "No active motorcycle selected."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = aiRepository.getMechanicSummary(bikeId, forceRefresh)
            result.onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = summary,
                    isAiEnabled = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

