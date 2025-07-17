package com.andychen.habitgem.data.repository

import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ReminderPreferences
import com.andychen.habitgem.domain.model.TimeSlot
import com.andychen.habitgem.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

/**
 * Repository for user preferences
 */
interface UserPreferencesRepository {
    /**
     * Get user preferences
     */
    fun getUserPreferences(userId: String): Flow<UserPreferences?>
    
    /**
     * Save user preferences
     */
    suspend fun saveUserPreferences(userId: String, preferences: UserPreferences)
    
    /**
     * Update preferred habit categories
     */
    suspend fun updatePreferredCategories(userId: String, categories: List<HabitCategory>)
    
    /**
     * Update preferred goal types
     */
    suspend fun updatePreferredGoalTypes(userId: String, goalTypes: List<GoalType>)
    
    /**
     * Update reminder preferences
     */
    suspend fun updateReminderPreferences(userId: String, reminderPreferences: ReminderPreferences)
    
    /**
     * Update difficulty preference
     */
    suspend fun updateDifficultyPreference(userId: String, difficultyPreference: Int)
    
    /**
     * Update time availability
     */
    suspend fun updateTimeAvailability(userId: String, timeAvailability: Map<DayOfWeek, List<TimeSlot>>)
    
    /**
     * Sync user preferences with remote server
     * @return true if sync was successful, false otherwise
     */
    suspend fun syncUserPreferences(userId: String): Boolean
    
    /**
     * Sync all pending user preferences with remote server
     * @return number of successfully synced preferences
     */
    suspend fun syncAllPendingPreferences(): Int
}