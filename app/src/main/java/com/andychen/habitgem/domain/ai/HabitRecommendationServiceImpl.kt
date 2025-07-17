package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.HabitRecommendationRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.HabitEvidence
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Implementation of HabitRecommendationService
 */
class HabitRecommendationServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : HabitRecommendationService {
    
    override suspend fun getInitialRecommendations(userPreferences: UserPreferences): List<HabitRecommendation> = withContext(Dispatchers.IO) {
        try {
            // Convert domain model to DTO
            val preferencesDto = userPreferences.toDto()
            
            // Make API request
            val response = aiServiceApi.getHabitRecommendations(
                HabitRecommendationRequest(
                    userId = "anonymous", // For initial recommendations, we don't have a user ID yet
                    preferences = preferencesDto,
                    existingHabits = null
                )
            )
            
            // Convert response to domain model
            return@withContext response.recommendations.map { it.toDomainModel() }
        } catch (e: Exception) {
            // Fallback to local recommendations if API fails
            return@withContext getFallbackRecommendations(userPreferences)
        }
    }
    
    override suspend fun getPersonalizedRecommendations(userId: String): List<HabitRecommendation> = withContext(Dispatchers.IO) {
        try {
            // Get existing habits
            val existingHabits = habitRepository.getHabitsByUserId(userId).first()
            val existingHabitIds = existingHabits.map { it.id }
            
            // Make API request
            val response = aiServiceApi.getHabitRecommendations(
                HabitRecommendationRequest(
                    userId = userId,
                    preferences = null, // Server will fetch preferences from user profile
                    existingHabits = existingHabitIds
                )
            )
            
            // Convert response to domain model
            return@withContext response.recommendations.map { it.toDomainModel() }
        } catch (e: Exception) {
            // Fallback to local recommendations if API fails
            return@withContext getLocalPersonalizedRecommendations(userId)
        }
    }
    
    override suspend fun getHabitEvidence(habitId: String): HabitEvidence = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, we would make an API call to get the evidence
            // For now, we'll return a placeholder
            return@withContext HabitEvidence(
                habitId = habitId,
                scientificBasis = "Research has shown that this habit can improve well-being and productivity.",
                references = listOf(
                    "Smith, J. et al. (2023). The Impact of Daily Habits on Well-being. Journal of Behavioral Science, 45(2), 112-128.",
                    "Johnson, A. & Williams, B. (2022). Habit Formation in Daily Life. Psychology Today, 78(3), 234-245."
                ),
                benefitsSummary = "Regular practice of this habit has been associated with improved mood, increased energy levels, and better overall health outcomes."
            )
        } catch (e: Exception) {
            // Return a generic evidence if API fails
            return@withContext HabitEvidence(
                habitId = habitId,
                scientificBasis = "This habit is based on established behavioral science principles.",
                references = emptyList(),
                benefitsSummary = "Regular practice may contribute to improved well-being."
            )
        }
    }
    
    /**
     * Fallback method to provide recommendations when API is unavailable
     */
    private fun getFallbackRecommendations(userPreferences: UserPreferences): List<HabitRecommendation> {
        // Return some generic recommendations based on user preferences
        val recommendations = mutableListOf<HabitRecommendation>()
        
        // Add recommendations based on preferred categories
        userPreferences.habitCategories.forEach { category ->
            when (category) {
                com.andychen.habitgem.domain.model.HabitCategory.HEALTH -> {
                    recommendations.add(
                        HabitRecommendation(
                            id = "health_water",
                            name = "每日饮水",
                            description = "每天喝8杯水，保持身体水分",
                            category = category,
                            difficulty = 1,
                            recommendationReason = "保持充足的水分对身体健康至关重要",
                            scientificBasis = "研究表明，充足的水分摄入可以改善认知功能，促进新陈代谢，并帮助排出体内毒素",
                            suggestedFrequency = com.andychen.habitgem.domain.model.Frequency.Daily(),
                            estimatedTimePerDay = 5
                        )
                    )
                }
                com.andychen.habitgem.domain.model.HabitCategory.FITNESS -> {
                    recommendations.add(
                        HabitRecommendation(
                            id = "fitness_stretch",
                            name = "晨间拉伸",
                            description = "每天早晨进行10分钟的全身拉伸",
                            category = category,
                            difficulty = 2,
                            recommendationReason = "早晨拉伸可以唤醒身体，提高灵活性",
                            scientificBasis = "定期拉伸可以改善肌肉灵活性，减少受伤风险，并促进血液循环",
                            suggestedFrequency = com.andychen.habitgem.domain.model.Frequency.Daily(),
                            estimatedTimePerDay = 10
                        )
                    )
                }
                com.andychen.habitgem.domain.model.HabitCategory.MINDFULNESS -> {
                    recommendations.add(
                        HabitRecommendation(
                            id = "mindfulness_meditation",
                            name = "冥想练习",
                            description = "每天进行10分钟的专注呼吸冥想",
                            category = category,
                            difficulty = 3,
                            recommendationReason = "冥想可以减轻压力，提高专注力",
                            scientificBasis = "研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡",
                            suggestedFrequency = com.andychen.habitgem.domain.model.Frequency.Daily(),
                            estimatedTimePerDay = 10
                        )
                    )
                }
                else -> {
                    // Add generic recommendation for other categories
                }
            }
        }
        
        return recommendations
    }
    
    /**
     * Generate personalized recommendations based on local data
     */
    private suspend fun getLocalPersonalizedRecommendations(userId: String): List<HabitRecommendation> {
        // In a real implementation, we would analyze user data to generate recommendations
        // For now, we'll return some generic recommendations
        return listOf(
            HabitRecommendation(
                id = "reading_daily",
                name = "每日阅读",
                description = "每天阅读20分钟，拓展知识面",
                category = com.andychen.habitgem.domain.model.HabitCategory.LEARNING,
                difficulty = 2,
                recommendationReason = "基于您的兴趣和目标，阅读习惯将帮助您持续学习和成长",
                scientificBasis = "研究表明，定期阅读可以提高认知能力，增强记忆力，并减轻压力",
                suggestedFrequency = com.andychen.habitgem.domain.model.Frequency.Daily(),
                estimatedTimePerDay = 20
            ),
            HabitRecommendation(
                id = "gratitude_journal",
                name = "感恩日记",
                description = "每晚记录三件感恩的事情",
                category = com.andychen.habitgem.domain.model.HabitCategory.MINDFULNESS,
                difficulty = 1,
                recommendationReason = "记录感恩事项可以培养积极心态，提高幸福感",
                scientificBasis = "心理学研究表明，感恩练习可以提高主观幸福感，改善睡眠质量，并增强心理韧性",
                suggestedFrequency = com.andychen.habitgem.domain.model.Frequency.Daily(),
                estimatedTimePerDay = 5
            )
        )
    }
    
    /**
     * Extension function to convert UserPreferences to DTO
     */
    private fun UserPreferences.toDto(): com.andychen.habitgem.data.api.model.UserPreferencesDto {
        return com.andychen.habitgem.data.api.model.UserPreferencesDto(
            habitCategories = this.habitCategories.map { it.name },
            goalTypes = this.goalTypes.map { it.name },
            difficultyPreference = this.difficultyPreference,
            timeAvailability = this.timeAvailability.mapKeys { it.key.name }
                .mapValues { entry ->
                    entry.value.map { timeSlot ->
                        com.andychen.habitgem.data.api.model.TimeSlotDto(
                            startTime = timeSlot.startTime.toString(),
                            endTime = timeSlot.endTime.toString()
                        )
                    }
                }
        )
    }
    
    /**
     * Extension function to convert HabitRecommendationDto to domain model
     */
    private fun com.andychen.habitgem.data.api.model.HabitRecommendationDto.toDomainModel(): HabitRecommendation {
        return HabitRecommendation(
            id = this.id,
            name = this.name,
            description = this.description,
            category = com.andychen.habitgem.domain.model.HabitCategory.valueOf(this.category),
            difficulty = this.difficulty,
            recommendationReason = this.recommendationReason,
            scientificBasis = this.scientificBasis,
            suggestedFrequency = this.suggestedFrequency.toDomainModel(),
            estimatedTimePerDay = this.estimatedTimePerDay
        )
    }
    
    /**
     * Extension function to convert FrequencyDto to domain model
     */
    private fun com.andychen.habitgem.data.api.model.FrequencyDto.toDomainModel(): com.andychen.habitgem.domain.model.Frequency {
        return when (this.type) {
            "DAILY" -> com.andychen.habitgem.domain.model.Frequency.Daily()
            "WEEKLY" -> com.andychen.habitgem.domain.model.Frequency.Weekly(
                this.daysOfWeek?.map { java.time.DayOfWeek.valueOf(it) } ?: emptyList()
            )
            "MONTHLY" -> com.andychen.habitgem.domain.model.Frequency.Monthly(
                this.daysOfWeek?.map { it.toInt() } ?: emptyList()
            )
            "INTERVAL" -> com.andychen.habitgem.domain.model.Frequency.Interval(
                this.timesPerWeek ?: 1
            )
            else -> com.andychen.habitgem.domain.model.Frequency.Daily()
        }
    }
}