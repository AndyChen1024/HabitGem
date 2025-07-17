package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.domain.model.HabitCorrelation
import com.andychen.habitgem.domain.model.HabitInsight
import com.andychen.habitgem.domain.model.OptimizationSuggestion

/**
 * Service for AI-powered habit analysis
 */
interface HabitAnalysisService {
    /**
     * Get insights for a specific habit
     * @param userId User ID
     * @param habitId Habit ID
     * @return Habit insights
     */
    suspend fun getHabitInsights(userId: String, habitId: String): HabitInsight
    
    /**
     * Get optimization suggestions for a habit
     * @param userId User ID
     * @param habitId Habit ID
     * @return List of optimization suggestions
     */
    suspend fun getOptimizationSuggestions(userId: String, habitId: String): List<OptimizationSuggestion>
    
    /**
     * Get correlations between habits
     * @param userId User ID
     * @return List of habit correlations
     */
    suspend fun getHabitCorrelations(userId: String): List<HabitCorrelation>
}