package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.domain.model.FeedbackMessage
import com.andychen.habitgem.domain.model.PeriodicReport
import com.andychen.habitgem.domain.model.ProgressAnalysis
import com.andychen.habitgem.domain.model.ReportPeriod
import java.time.LocalDate

/**
 * Service for AI-powered progress feedback
 */
interface ProgressFeedbackService {
    /**
     * Get feedback message when a habit is completed
     * @param userId User ID
     * @param habitId Habit ID
     * @return Feedback message
     */
    suspend fun getCompletionFeedback(userId: String, habitId: String): FeedbackMessage
    
    /**
     * Get progress analysis for a habit
     * @param userId User ID
     * @param habitId Habit ID
     * @return Progress analysis
     */
    suspend fun getProgressAnalysis(userId: String, habitId: String): ProgressAnalysis
    
    /**
     * Get periodic report for all habits
     * @param userId User ID
     * @param period Report period (daily, weekly, monthly)
     * @param endDate End date for the report (defaults to today)
     * @return Periodic report
     */
    suspend fun getPeriodicReport(
        userId: String,
        period: ReportPeriod,
        endDate: LocalDate = LocalDate.now()
    ): PeriodicReport
}