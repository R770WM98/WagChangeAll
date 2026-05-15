package com.thisismine.myapplication.feature.predictive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thisismine.myapplication.ai.AiRepository
import com.thisismine.myapplication.ai.PredictiveRulesSuggestion
import com.thisismine.myapplication.feature.reminders.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PredictiveRulesUiState(
    val isLoading: Boolean = false,
    val suggestions: List<PredictiveRulesSuggestion> = emptyList(),
    val approvedSuggestions: List<PredictiveRulesSuggestion> = emptyList(),
    val error: String? = null,
    val isAiEnabled: Boolean = false,
    val analysisNotes: String? = null,
    val isSaving: Boolean = false,
    val saveMessage: String? = null
)

class PredictiveRulesViewModel(
    private val aiRepository: AiRepository,
    private val reminderRepository: ReminderRepository,
    private val activeBikeId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PredictiveRulesUiState())
    val uiState = _uiState.asStateFlow()

    fun generateRules(forceRefresh: Boolean = false) {
        val bikeId = activeBikeId ?: run {
            _uiState.value = _uiState.value.copy(
                error = "No active motorcycle selected."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = aiRepository.getPredictiveRules(bikeId, forceRefresh)
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    suggestions = response.suggestions,
                    isAiEnabled = true,
                    analysisNotes = response.analysisNotes
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unknown error"
                )
            }
        }
    }

    fun approveSuggestion(suggestion: PredictiveRulesSuggestion) {
        val currentApproved = _uiState.value.approvedSuggestions.toMutableList()
        if (!currentApproved.contains(suggestion)) {
            currentApproved.add(suggestion)
            _uiState.value = _uiState.value.copy(approvedSuggestions = currentApproved)
        }
    }

    fun rejectSuggestion(suggestion: PredictiveRulesSuggestion) {
        val current = _uiState.value.suggestions.toMutableList()
        current.remove(suggestion)
        _uiState.value = _uiState.value.copy(suggestions = current)
    }

    fun saveApprovedRules() {
        if (_uiState.value.approvedSuggestions.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No approved rules to save.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, saveMessage = null)
            try {
                val currentOdometer = reminderRepository.getCurrentOdometerKm()
                var successCount = 0

                _uiState.value.approvedSuggestions.forEach { suggestion ->
                    val (triggerKm, triggerDays) = parseInterval(suggestion.suggestedInterval)
                    
                    // Calculate trigger point: current odometer + suggested km
                    val reminderTriggerKm = if (triggerKm > 0) currentOdometer + triggerKm else currentOdometer
                    
                    // Calculate trigger date: today + suggested days
                    val reminderTriggerDate = if (triggerDays > 0) {
                        LocalDate.now().plusDays(triggerDays.toLong()).toString()
                    } else {
                        null
                    }

                    // Create reminder with title based on the suggestion
                    val reminderTitle = "AI Suggested: ${suggestion.component}"
                    
                    try {
                        reminderRepository.addReminder(
                            title = reminderTitle,
                            triggerAtKm = reminderTriggerKm,
                            triggerAtIsoDate = reminderTriggerDate,
                            repeatingIntervalKm = if (triggerKm > 0) triggerKm else null,
                            lastTriggeredAtKm = null,
                            completionCount = 0,
                            lastCompletedAtIsoDate = null,
                            isArchived = false
                        )
                        successCount++
                    } catch (e: Exception) {
                        // Log but continue with other suggestions
                        android.util.Log.e("PredictiveRulesVM", "Failed to create reminder: ${suggestion.component}", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveMessage = "Successfully saved $successCount reminder${if (successCount != 1) "s" else ""}",
                    approvedSuggestions = emptyList(),
                    suggestions = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save rules: ${e.message}"
                )
            }
        }
    }

    /**
     * Parse interval strings like "6000 km or 6 months" into (km, days).
     * Returns Pair<km: Int, days: Int>. If a component is not found, 0 is used.
     */
    private fun parseInterval(intervalStr: String): Pair<Int, Int> {
        var km = 0
        var days = 0

        // Replace unicode dash variants with regular dash
        val normalized = intervalStr.lowercase()
            .replace("–", "-")
            .replace("—", "-")

        // Try to extract km value: "6000 km" or "6,000 km"
        val kmPattern = Regex("""(\d+(?:,\d+)?)\s*km""")
        val kmMatch = kmPattern.find(normalized)
        if (kmMatch != null) {
            km = kmMatch.groupValues[1].replace(",", "").toIntOrNull() ?: 0
        }

        // Try to extract month/time value: "6 months" or "6 month", or "6-12 months"
        val monthPattern = Regex("""(\d+)(?:-\d+)?\s*months?""")
        val monthMatch = monthPattern.find(normalized)
        if (monthMatch != null) {
            val months = monthMatch.groupValues[1].toIntOrNull() ?: 0
            // Convert months to approximate days (30 days per month)
            days = months * 30
        }

        return Pair(km, days)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

