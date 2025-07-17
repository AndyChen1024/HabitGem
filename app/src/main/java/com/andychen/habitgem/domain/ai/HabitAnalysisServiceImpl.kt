package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.HabitAnalysisRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.CorrelationType
import com.andychen.habitgem.domain.model.HabitCorrelation
import com.andychen.habitgem.domain.model.HabitInsight
import com.andychen.habitgem.domain.model.OptimizationSuggestion
import com.andychen.habitgem.domain.model.SuggestionType
import com.andychen.habitgem.domain.model.Trend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Implementation of HabitAnalysisService
 */
class HabitAnalysisServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : HabitAnalysisService {
    
    override suspend fun getHabitInsights(userId: String, habitId: String): HabitInsight = withContext(Dispatchers.IO) {
        try {
            // Make API request
            val response = aiServiceApi.getHabitAnalysis(
                HabitAnalysisRequest(
                    userId = userId,
                    habitId = habitId,
                    analysisType = "INSIGHTS",
                    timeRange = null // Use default time range
                )
            )
            
            // Get the first insight from the response
            val insightDto = response.insights.firstOrNull() ?: throw Exception("No insights available")
            
            // Convert to domain model
            return@withContext HabitInsight(
                habitId = insightDto.habitId,
                bestPerformingDays = insightDto.bestPerformingDays.map { DayOfWeek.valueOf(it) },
                completionTrend = Trend.valueOf(insightDto.completionTrend),
                consistencyScore = insightDto.consistencyScore,
                insightMessage = insightDto.insightMessage
            )
        } catch (e: Exception) {
            // Fallback to local insights if API fails
            return@withContext generateLocalInsights(userId, habitId)
        }
    }
    
    override suspend fun getOptimizationSuggestions(userId: String, habitId: String): List<OptimizationSuggestion> = withContext(Dispatchers.IO) {
        try {
            // Make API request
            val response = aiServiceApi.getHabitAnalysis(
                HabitAnalysisRequest(
                    userId = userId,
                    habitId = habitId,
                    analysisType = "OPTIMIZATION",
                    timeRange = null // Use default time range
                )
            )
            
            // Convert to domain model
            return@withContext response.suggestions?.map {
                OptimizationSuggestion(
                    type = SuggestionType.valueOf(it.type),
                    message = it.message,
                    expectedImpact = it.expectedImpact,
                    confidence = it.confidence
                )
            } ?: emptyList()
        } catch (e: Exception) {
            // Fallback to local suggestions if API fails
            return@withContext generateLocalSuggestions(userId, habitId)
        }
    }
    
    override suspend fun getHabitCorrelations(userId: String): List<HabitCorrelation> = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, we would make an API call to get correlations
            // For now, we'll generate some sample correlations
            val habits = habitRepository.getHabitsByUserId(userId).first()
            
            // We need at least 2 habits to find correlations
            if (habits.size < 2) {
                return@withContext emptyList()
            }
            
            // Generate sample correlations between pairs of habits
            val correlations = mutableListOf<HabitCorrelation>()
            for (i in 0 until habits.size - 1) {
                for (j in i + 1 until habits.size) {
                    val habit1 = habits[i]
                    val habit2 = habits[j]
                    
                    // Generate a random correlation strength between -1.0 and 1.0
                    val strength = (Math.random() * 2 - 1).toFloat()
                    
                    val correlationType = when {
                        strength > 0.3 -> CorrelationType.POSITIVE
                        strength < -0.3 -> CorrelationType.NEGATIVE
                        else -> CorrelationType.NEUTRAL
                    }
                    
                    val description = when (correlationType) {
                        CorrelationType.POSITIVE -> "完成"${habit1.name}"后，您更有可能完成"${habit2.name}"。"
                        CorrelationType.NEGATIVE -> "在完成"${habit1.name}"的日子里，您较少完成"${habit2.name}"。"
                        CorrelationType.NEUTRAL -> "这两个习惯之间没有明显的关联。"
                    }
                    
                    correlations.add(
                        HabitCorrelation(
                            habitId1 = habit1.id,
                            habitId2 = habit2.id,
                            correlationType = correlationType,
                            correlationStrength = strength,
                            description = description
                        )
                    )
                }
            }
            
            // Return only strong correlations
            return@withContext correlations.filter { 
                Math.abs(it.correlationStrength) > 0.5 
            }
        } catch (e: Exception) {
            // Return empty list if there's an error
            return@withContext emptyList()
        }
    }
    
    /**
     * Generate local insights based on habit data
     */
    private suspend fun generateLocalInsights(userId: String, habitId: String): HabitInsight {
        // Get habit records
        val records = habitRepository.getHabitRecords(habitId).first()
        
        // If there are no records, return default insights
        if (records.isEmpty()) {
            return HabitInsight(
                habitId = habitId,
                bestPerformingDays = emptyList(),
                completionTrend = Trend.NOT_ENOUGH_DATA,
                consistencyScore = 0f,
                insightMessage = "继续记录您的习惯，我们将为您提供更详细的分析。"
            )
        }
        
        // Calculate best performing days
        val dayCompletionCounts = records
            .filter { it.isCompleted }
            .groupBy { it.date.dayOfWeek }
            .mapValues { it.value.size }
        
        val bestDays = if (dayCompletionCounts.isNotEmpty()) {
            val maxCount = dayCompletionCounts.maxOf { it.value }
            dayCompletionCounts.filter { it.value == maxCount }.keys.toList()
        } else {
            emptyList()
        }
        
        // Calculate completion trend
        val trend = if (records.size < 7) {
            Trend.NOT_ENOUGH_DATA
        } else {
            // Compare recent completion rate with earlier completion rate
            val midpoint = records.size / 2
            val recentRecords = records.take(midpoint)
            val earlierRecords = records.drop(midpoint)
            
            val recentCompletionRate = recentRecords.count { it.isCompleted }.toFloat() / recentRecords.size
            val earlierCompletionRate = earlierRecords.count { it.isCompleted }.toFloat() / earlierRecords.size
            
            when {
                recentCompletionRate > earlierCompletionRate * 1.1 -> Trend.IMPROVING
                recentCompletionRate < earlierCompletionRate * 0.9 -> Trend.DECLINING
                else -> Trend.STABLE
            }
        }
        
        // Calculate consistency score (0.0 - 1.0)
        val consistencyScore = if (records.isNotEmpty()) {
            records.count { it.isCompleted }.toFloat() / records.size
        } else {
            0f
        }
        
        // Generate insight message
        val insightMessage = when {
            trend == Trend.NOT_ENOUGH_DATA -> 
                "继续记录您的习惯，我们将为您提供更详细的分析。"
            trend == Trend.IMPROVING && consistencyScore > 0.7 -> 
                "您的习惯坚持度正在提高，并且保持了很高的一致性。太棒了！"
            trend == Trend.IMPROVING -> 
                "您的习惯坚持度正在提高，继续保持这个势头！"
            trend == Trend.DECLINING && consistencyScore < 0.3 -> 
                "您的习惯坚持度有所下降，考虑调整习惯的难度或设置提醒。"
            trend == Trend.DECLINING -> 
                "您的习惯坚持度最近有所下降，尝试找出原因并做出调整。"
            bestDays.isNotEmpty() -> 
                "您在${bestDays.joinToString(", ") { it.name }}表现最好，考虑在这些天安排更多习惯。"
            else -> 
                "您的习惯坚持度保持稳定，继续保持！"
        }
        
        return HabitInsight(
            habitId = habitId,
            bestPerformingDays = bestDays,
            completionTrend = trend,
            consistencyScore = consistencyScore,
            insightMessage = insightMessage
        )
    }
    
    /**
     * Generate local optimization suggestions
     */
    private suspend fun generateLocalSuggestions(userId: String, habitId: String): List<OptimizationSuggestion> {
        // Get habit and records
        val habit = habitRepository.getHabitById(habitId).first() ?: return emptyList()
        val records = habitRepository.getHabitRecords(habitId).first()
        
        // If there are no records, return default suggestions
        if (records.isEmpty()) {
            return listOf(
                OptimizationSuggestion(
                    type = SuggestionType.TIME_CHANGE,
                    message = "尝试在早晨完成这个习惯，研究表明这可以提高成功率。",
                    expectedImpact = "可能提高完成率15-20%",
                    confidence = 0.7f
                )
            )
        }
        
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Calculate completion rate
        val completionRate = records.count { it.isCompleted }.toFloat() / records.size
        
        // Add suggestions based on completion rate
        if (completionRate < 0.5) {
            // If completion rate is low, suggest adjusting difficulty
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.DIFFICULTY_ADJUST,
                    message = "这个习惯的完成率较低，考虑降低难度或减少频率。",
                    expectedImpact = "可能提高完成率20-30%",
                    confidence = 0.8f
                )
            )
        }
        
        // Suggest time change if there's a pattern in missed days
        val missedDays = records
            .filter { !it.isCompleted }
            .groupBy { it.date.dayOfWeek }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }
        
        if (missedDays.isNotEmpty()) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.TIME_CHANGE,
                    message = "您在${missedDays.joinToString("和") { it.name }}经常错过这个习惯，考虑调整这些天的时间安排。",
                    expectedImpact = "可能提高这些天的完成率15-25%",
                    confidence = 0.75f
                )
            )
        }
        
        // Add a general suggestion
        suggestions.add(
            OptimizationSuggestion(
                type = SuggestionType.HABIT_COMBINATION,
                message = "尝试将这个习惯与您已经养成的习惯结合起来，例如在刷牙后立即完成。",
                expectedImpact = "可能提高习惯的自动化程度",
                confidence = 0.65f
            )
        )
        
        return suggestions
    }
}