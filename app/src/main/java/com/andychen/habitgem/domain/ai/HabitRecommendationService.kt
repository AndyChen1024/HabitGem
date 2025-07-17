package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.domain.model.HabitEvidence
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.domain.model.UserPreferences

/**
 * Service for AI-powered habit recommendations
 */
interface HabitRecommendationService {
    /**
     * Get initial habit recommendations based on user preferences
     * @param userPreferences User's preferences for habits
     * @return List of recommended habits
     */
    suspend fun getInitialRecommendations(userPreferences: UserPreferences): List<HabitRecommendation>
    
    /**
     * Get personalized habit recommendations based on user's existing habits and behavior
     * @param userId User ID
     * @return List of recommended habits
     */
    suspend fun getPersonalizedRecommendations(userId: String): List<HabitRecommendation>
    
    /**
     * Get scientific evidence for a specific habit
     * @param habitId Habit ID
     * @return Scientific evidence for the habit
     */
    suspend fun getHabitEvidence(habitId: String): HabitEvidence
}