package com.andychen.habitgem.ui.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andychen.habitgem.data.repository.UserPreferencesRepository
import com.andychen.habitgem.domain.ai.HabitRecommendationService
import com.andychen.habitgem.domain.model.HabitEvidence
import com.andychen.habitgem.domain.model.HabitRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the habit recommendation screen
 */
data class HabitRecommendationUiState(
    val recommendations: List<HabitRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAiRecommendations: Boolean = true,
    val selectedRecommendation: HabitRecommendation? = null,
    val habitEvidence: HabitEvidence? = null,
    val isLoadingEvidence: Boolean = false,
    val loadedEvidenceIds: Set<String> = emptySet() // Track which habit IDs have evidence loaded
)

/**
 * ViewModel for the habit recommendation screen
 */
@HiltViewModel
class HabitRecommendationViewModel @Inject constructor(
    private val habitRecommendationService: HabitRecommendationService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HabitRecommendationUiState(isLoading = true))
    val uiState: StateFlow<HabitRecommendationUiState> = _uiState.asStateFlow()
    
    init {
        loadRecommendations()
    }
    
    /**
     * Load habit recommendations
     */
    fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get user ID
                val userId = userPreferencesRepository.getUserId().first()
                
                // Get recommendations
                val recommendations = if (userId.isNotEmpty()) {
                    // Get personalized recommendations if user ID exists
                    habitRecommendationService.getPersonalizedRecommendations(userId)
                } else {
                    // Get initial recommendations based on user preferences
                    val userPreferences = userPreferencesRepository.getUserPreferences().first()
                    habitRecommendationService.getInitialRecommendations(userPreferences)
                }
                
                _uiState.update { 
                    it.copy(
                        recommendations = recommendations,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载推荐时出错"
                    )
                }
            }
        }
    }
    
    /**
     * Set whether to show AI recommendations or manual creation
     */
    fun setShowAiRecommendations(show: Boolean) {
        _uiState.update { it.copy(showAiRecommendations = show) }
    }
    
    /**
     * Show habit details dialog
     */
    fun showHabitDetails(recommendation: HabitRecommendation) {
        _uiState.update { it.copy(selectedRecommendation = recommendation, habitEvidence = null) }
    }
    
    /**
     * Dismiss habit details dialog
     */
    fun dismissHabitDetails() {
        _uiState.update { it.copy(selectedRecommendation = null, habitEvidence = null) }
    }
    
    /**
     * Load scientific evidence for a habit
     */
    fun loadHabitEvidence(habitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEvidence = true) }
            
            try {
                val evidence = habitRecommendationService.getHabitEvidence(habitId)
                _uiState.update { currentState -> 
                    currentState.copy(
                        habitEvidence = evidence,
                        isLoadingEvidence = false,
                        // Add this habit ID to the set of loaded evidence IDs
                        loadedEvidenceIds = currentState.loadedEvidenceIds + habitId
                    )
                }
            } catch (e: Exception) {
                // If there's an error, we just don't show the evidence
                _uiState.update { it.copy(isLoadingEvidence = false) }
            }
        }
    }
    
    /**
     * Check if evidence is available for a habit
     */
    fun hasLoadedEvidence(habitId: String): Boolean {
        return _uiState.value.loadedEvidenceIds.contains(habitId)
    }
}