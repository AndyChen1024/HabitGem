package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.ProgressFeedbackRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.AnimationType
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.domain.model.FeedbackMessage
import com.andychen.habitgem.domain.model.FeedbackType
import com.andychen.habitgem.domain.model.PeriodicReport
import com.andychen.habitgem.domain.model.ProgressAnalysis
import com.andychen.habitgem.domain.model.ReportPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Implementation of ProgressFeedbackService
 */
class ProgressFeedbackServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : ProgressFeedbackService {
    
    override suspend fun getCompletionFeedback(userId: String, habitId: String): FeedbackMessage = withContext(Dispatchers.IO) {
        try {
            // Get current streak
            val streak = habitRepository.getCurrentStreak(habitId).first()
            
            // Determine feedback type based on streak
            val feedbackType = when {
                streak >= 30 -> FeedbackType.MILESTONE
                streak >= 7 -> FeedbackType.STREAK
                else -> FeedbackType.COMPLETION
            }
            
            // Create context data
            val contextData = mapOf(
                "streak" to streak.toString(),
                "habit_id" to habitId
            )
            
            // Make API request
            val response = aiServiceApi.getProgressFeedback(
                ProgressFeedbackRequest(
                    userId = userId,
                    habitId = habitId,
                    feedbackType = feedbackType.name,
                    contextData = contextData
                )
            )
            
            // Convert response to domain model
            return@withContext FeedbackMessage(
                message = response.feedback.message,
                type = FeedbackType.valueOf(response.feedback.type),
                emoji = response.feedback.emoji,
                animationType = response.feedback.animationType?.let { AnimationType.valueOf(it) }
            )
        } catch (e: Exception) {
            // Fallback to local feedback if API fails
            return@withContext getFallbackCompletionFeedback(streak = habitRepository.getCurrentStreak(habitId).first())
        }
    }
    
    override suspend fun getProgressAnalysis(userId: String, habitId: String): ProgressAnalysis = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, we would make an API call to get the analysis
            // For now, we'll return a placeholder
            val completionRate = habitRepository.getCompletionRate(habitId).first()
            val streak = habitRepository.getCurrentStreak(habitId).first()
            
            // Generate sample data points for the last 7 days
            val today = LocalDate.now()
            val dataPoints = (0..6).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val record = habitRepository.getHabitRecordForDate(habitId, date).first()
                DataPoint(
                    date = date,
                    value = if (record?.isCompleted == true) 1f else 0f,
                    label = date.dayOfWeek.name.substring(0, 3)
                )
            }.reversed()
            
            return@withContext ProgressAnalysis(
                completionRate = completionRate,
                streak = streak,
                insight = "您在周二和周四的完成率最高，建议继续保持这个规律。",
                suggestion = "尝试在早晨完成这个习惯，研究表明这可以提高成功率。",
                visualData = dataPoints
            )
        } catch (e: Exception) {
            // Return a generic analysis if there's an error
            return@withContext ProgressAnalysis(
                completionRate = 0f,
                streak = 0,
                insight = "继续坚持，数据将帮助我们提供更准确的分析。",
                suggestion = "尝试设置提醒，帮助您更好地坚持习惯。",
                visualData = emptyList()
            )
        }
    }
    
    override suspend fun getPeriodicReport(
        userId: String,
        period: ReportPeriod,
        endDate: LocalDate
    ): PeriodicReport = withContext(Dispatchers.IO) {
        try {
            // Calculate start date based on period
            val startDate = when (period) {
                ReportPeriod.DAILY -> endDate
                ReportPeriod.WEEKLY -> endDate.minusDays(6) // Last 7 days
                ReportPeriod.MONTHLY -> endDate.minusDays(29) // Last 30 days
            }
            
            // Get all habits for the user
            val habits = habitRepository.getHabitsByUserId(userId).first()
            
            // Calculate completion rates for each habit
            val habitsSummary = habits.associate { habit ->
                habit.id to habitRepository.getCompletionRateForDateRange(
                    habitId = habit.id,
                    startDate = startDate,
                    endDate = endDate
                ).first()
            }
            
            // Calculate overall completion rate
            val overallCompletionRate = if (habitsSummary.isNotEmpty()) {
                habitsSummary.values.sum() / habitsSummary.size
            } else {
                0f
            }
            
            // Generate insights and recommendations
            val insights = generateInsights(habitsSummary)
            val recommendations = generateRecommendations(habitsSummary)
            
            return@withContext PeriodicReport(
                period = period,
                startDate = startDate,
                endDate = endDate,
                completionRate = overallCompletionRate,
                habitsSummary = habitsSummary,
                insights = insights,
                recommendations = recommendations
            )
        } catch (e: Exception) {
            // Return a generic report if there's an error
            return@withContext PeriodicReport(
                period = period,
                startDate = endDate.minus(7, ChronoUnit.DAYS),
                endDate = endDate,
                completionRate = 0f,
                habitsSummary = emptyMap(),
                insights = listOf("继续记录您的习惯，我们将为您提供更详细的分析。"),
                recommendations = listOf("尝试每天固定时间完成习惯，这有助于建立稳定的习惯模式。")
            )
        }
    }
    
    /**
     * Generate fallback completion feedback
     */
    private fun getFallbackCompletionFeedback(streak: Int): FeedbackMessage {
        return when {
            streak >= 30 -> {
                FeedbackMessage(
                    message = "太棒了！您已经连续坚持30天，这是一个重要的里程碑！",
                    type = FeedbackType.MILESTONE,
                    emoji = "🏆",
                    animationType = AnimationType.FIREWORKS
                )
            }
            streak >= 7 -> {
                FeedbackMessage(
                    message = "恭喜您连续坚持${streak}天！保持这个势头！",
                    type = FeedbackType.STREAK,
                    emoji = "🔥",
                    animationType = AnimationType.SPARKLE
                )
            }
            else -> {
                FeedbackMessage(
                    message = "做得好！坚持是成功的关键。",
                    type = FeedbackType.COMPLETION,
                    emoji = "👍",
                    animationType = AnimationType.THUMBS_UP
                )
            }
        }
    }
    
    /**
     * Generate insights based on habit completion rates
     */
    private fun generateInsights(habitsSummary: Map<String, Float>): List<String> {
        val insights = mutableListOf<String>()
        
        if (habitsSummary.isEmpty()) {
            insights.add("您还没有足够的数据来生成洞察。")
            return insights
        }
        
        // Add overall insight
        val averageCompletion = habitsSummary.values.average()
        when {
            averageCompletion >= 0.8 -> {
                insights.add("您的整体完成率非常高，继续保持这个良好的习惯！")
            }
            averageCompletion >= 0.5 -> {
                insights.add("您的整体完成率良好，但还有提升空间。")
            }
            else -> {
                insights.add("您的整体完成率有待提高，尝试设置更小、更容易实现的目标。")
            }
        }
        
        // Add habit-specific insights
        val bestHabit = habitsSummary.entries.maxByOrNull { it.value }
        val worstHabit = habitsSummary.entries.minByOrNull { it.value }
        
        bestHabit?.let {
            if (it.value > 0.7) {
                insights.add("您在某个习惯上表现特别出色，这表明您已经开始建立稳定的习惯模式。")
            }
        }
        
        worstHabit?.let {
            if (it.value < 0.3) {
                insights.add("有一个习惯的完成率较低，考虑调整它的难度或时间安排。")
            }
        }
        
        return insights
    }
    
    /**
     * Generate recommendations based on habit completion rates
     */
    private fun generateRecommendations(habitsSummary: Map<String, Float>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (habitsSummary.isEmpty()) {
            recommendations.add("开始记录您的习惯，我们将为您提供个性化的建议。")
            return recommendations
        }
        
        // Add general recommendations
        recommendations.add("尝试在固定的时间完成习惯，这有助于建立稳定的习惯模式。")
        recommendations.add("使用视觉提示（如便利贴或手机提醒）来增加习惯的触发因素。")
        
        // Add habit-specific recommendations
        val lowCompletionHabits = habitsSummary.filter { it.value < 0.5 }
        if (lowCompletionHabits.isNotEmpty()) {
            recommendations.add("对于完成率较低的习惯，考虑将其分解为更小的步骤，或调整时间安排。")
        }
        
        return recommendations
    }
}