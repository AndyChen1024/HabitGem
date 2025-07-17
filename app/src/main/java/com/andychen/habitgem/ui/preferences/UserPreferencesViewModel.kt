package com.andychen.habitgem.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andychen.habitgem.data.repository.UserPreferencesRepository
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ReminderPreferences
import com.andychen.habitgem.domain.model.TimeSlot
import com.andychen.habitgem.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel for the user preferences questionnaire
 */
@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // UI state for the questionnaire
    private val _uiState = MutableStateFlow(UserPreferencesUiState())
    val uiState: StateFlow<UserPreferencesUiState> = _uiState.asStateFlow()

    // Current user ID - in a real app, this would come from authentication
    private var currentUserId: String = "current_user"

    init {
        // Load existing preferences if available
        loadUserPreferences()
    }

    /**
     * Load user preferences from repository
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.getUserPreferences(currentUserId).collect { preferences ->
                preferences?.let {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            selectedCategories = it.habitCategories.toMutableList(),
                            selectedGoalTypes = it.goalTypes.toMutableList(),
                            reminderEnabled = it.reminderPreferences.enabled,
                            reminderTime = it.reminderPreferences.defaultTime ?: LocalTime.of(8, 0),
                            reminderSound = it.reminderPreferences.notificationSound ?: "default",
                            vibrationEnabled = it.reminderPreferences.vibrationEnabled,
                            difficultyPreference = it.difficultyPreference,
                            timeAvailability = it.timeAvailability,
                            currentStep = QuestionnaireStep.CATEGORIES,
                            isComplete = true
                        )
                    }
                } ?: run {
                    // If no preferences exist, set default values
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * Toggle selection of a habit category
     */
    fun toggleCategory(category: HabitCategory) {
        _uiState.update { currentState ->
            val updatedCategories = currentState.selectedCategories.toMutableList()
            if (updatedCategories.contains(category)) {
                updatedCategories.remove(category)
            } else {
                updatedCategories.add(category)
            }
            currentState.copy(selectedCategories = updatedCategories)
        }
    }

    /**
     * Toggle selection of a goal type
     */
    fun toggleGoalType(goalType: GoalType) {
        _uiState.update { currentState ->
            val updatedGoalTypes = currentState.selectedGoalTypes.toMutableList()
            if (updatedGoalTypes.contains(goalType)) {
                updatedGoalTypes.remove(goalType)
            } else {
                updatedGoalTypes.add(goalType)
            }
            currentState.copy(selectedGoalTypes = updatedGoalTypes)
        }
    }

    /**
     * Update reminder settings
     */
    fun updateReminderSettings(
        enabled: Boolean? = null,
        time: LocalTime? = null,
        sound: String? = null,
        vibration: Boolean? = null
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                reminderEnabled = enabled ?: currentState.reminderEnabled,
                reminderTime = time ?: currentState.reminderTime,
                reminderSound = sound ?: currentState.reminderSound,
                vibrationEnabled = vibration ?: currentState.vibrationEnabled
            )
        }
    }

    /**
     * Update difficulty preference
     */
    fun updateDifficultyPreference(difficulty: Int) {
        _uiState.update { currentState ->
            currentState.copy(difficultyPreference = difficulty)
        }
    }

    /**
     * Update time availability for a specific day
     */
    fun updateTimeAvailability(day: DayOfWeek, timeSlots: List<TimeSlot>) {
        _uiState.update { currentState ->
            val updatedAvailability = currentState.timeAvailability.toMutableMap()
            updatedAvailability[day] = timeSlots
            currentState.copy(timeAvailability = updatedAvailability)
        }
    }

    /**
     * Move to the next step in the questionnaire
     */
    fun nextStep() {
        _uiState.update { currentState ->
            val nextStep = when (currentState.currentStep) {
                QuestionnaireStep.CATEGORIES -> QuestionnaireStep.GOALS
                QuestionnaireStep.GOALS -> QuestionnaireStep.DIFFICULTY
                QuestionnaireStep.DIFFICULTY -> QuestionnaireStep.REMINDERS
                QuestionnaireStep.REMINDERS -> QuestionnaireStep.TIME_AVAILABILITY
                QuestionnaireStep.TIME_AVAILABILITY -> QuestionnaireStep.SUMMARY
                QuestionnaireStep.SUMMARY -> QuestionnaireStep.SUMMARY
            }
            currentState.copy(currentStep = nextStep)
        }
    }

    /**
     * Move to the previous step in the questionnaire
     */
    fun previousStep() {
        _uiState.update { currentState ->
            val prevStep = when (currentState.currentStep) {
                QuestionnaireStep.CATEGORIES -> QuestionnaireStep.CATEGORIES
                QuestionnaireStep.GOALS -> QuestionnaireStep.CATEGORIES
                QuestionnaireStep.DIFFICULTY -> QuestionnaireStep.GOALS
                QuestionnaireStep.REMINDERS -> QuestionnaireStep.DIFFICULTY
                QuestionnaireStep.TIME_AVAILABILITY -> QuestionnaireStep.REMINDERS
                QuestionnaireStep.SUMMARY -> QuestionnaireStep.TIME_AVAILABILITY
            }
            currentState.copy(currentStep = prevStep)
        }
    }

    /**
     * Save all user preferences
     */
    fun savePreferences() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Create UserPreferences object
            val preferences = UserPreferences(
                habitCategories = currentState.selectedCategories,
                goalTypes = currentState.selectedGoalTypes,
                reminderPreferences = ReminderPreferences(
                    enabled = currentState.reminderEnabled,
                    defaultTime = if (currentState.reminderEnabled) currentState.reminderTime else null,
                    notificationSound = if (currentState.reminderEnabled) currentState.reminderSound else null,
                    vibrationEnabled = currentState.vibrationEnabled
                ),
                difficultyPreference = currentState.difficultyPreference,
                timeAvailability = currentState.timeAvailability
            )
            
            // Save to repository
            userPreferencesRepository.saveUserPreferences(currentUserId, preferences)
            
            // Update UI state
            _uiState.update { it.copy(isComplete = true, isSaving = false) }
        }
    }

    /**
     * Check if the current step is valid and can proceed
     */
    fun canProceedFromCurrentStep(): Boolean {
        return when (_uiState.value.currentStep) {
            QuestionnaireStep.CATEGORIES -> _uiState.value.selectedCategories.isNotEmpty()
            QuestionnaireStep.GOALS -> _uiState.value.selectedGoalTypes.isNotEmpty()
            QuestionnaireStep.DIFFICULTY -> true // Always valid
            QuestionnaireStep.REMINDERS -> true // Always valid
            QuestionnaireStep.TIME_AVAILABILITY -> _uiState.value.timeAvailability.isNotEmpty()
            QuestionnaireStep.SUMMARY -> true // Always valid
        }
    }
}

/**
 * UI state for the user preferences questionnaire
 */
data class UserPreferencesUiState(
    val isLoading: Boolean = true,
    val selectedCategories: List<HabitCategory> = listOf(
        HabitCategory.HEALTH,
        HabitCategory.MINDFULNESS,
        HabitCategory.PRODUCTIVITY
    ),
    val selectedGoalTypes: List<GoalType> = listOf(
        GoalType.HEALTH_IMPROVEMENT,
        GoalType.STRESS_REDUCTION,
        GoalType.PRODUCTIVITY_BOOST
    ),
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val reminderSound: String = "default",
    val vibrationEnabled: Boolean = true,
    val difficultyPreference: Int = 3,
    val timeAvailability: Map<DayOfWeek, List<TimeSlot>> = mapOf(
        DayOfWeek.MONDAY to listOf(
            TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
            TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.TUESDAY to listOf(
            TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
            TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.WEDNESDAY to listOf(
            TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
            TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.THURSDAY to listOf(
            TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
            TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.FRIDAY to listOf(
            TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
            TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.SATURDAY to listOf(
            TimeSlot(LocalTime.of(8, 0), LocalTime.of(12, 0)),
            TimeSlot(LocalTime.of(15, 0), LocalTime.of(22, 0))
        ),
        DayOfWeek.SUNDAY to listOf(
            TimeSlot(LocalTime.of(8, 0), LocalTime.of(12, 0)),
            TimeSlot(LocalTime.of(15, 0), LocalTime.of(22, 0))
        )
    ),
    val currentStep: QuestionnaireStep = QuestionnaireStep.CATEGORIES,
    val isComplete: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Steps in the user preferences questionnaire
 */
enum class QuestionnaireStep {
    CATEGORIES,
    GOALS,
    DIFFICULTY,
    REMINDERS,
    TIME_AVAILABILITY,
    SUMMARY
}