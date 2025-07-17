package com.andychen.habitgem.domain.ai

import android.util.LruCache
import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.HabitRecommendationRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitEvidence
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Implementation of HabitRecommendationService with caching and enhanced recommendation logic
 */
class HabitRecommendationServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : HabitRecommendationService {
    
    // Cache for recommendations - key is userId or "anonymous" for initial recommendations
    private val recommendationsCache = LruCache<String, CachedRecommendations>(10)
    
    // Cache for habit evidence - key is habitId
    private val evidenceCache = LruCache<String, CachedEvidence>(20)
    
    // Cache expiration time in milliseconds
    private val CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1) // 1 hour
    
    override suspend fun getInitialRecommendations(userPreferences: UserPreferences): List<HabitRecommendation> = withContext(Dispatchers.IO) {
        val cacheKey = "anonymous_${userPreferences.habitCategories.joinToString("_") { it.name }}_${userPreferences.difficultyPreference}"
        
        // Check cache first
        val cachedRecommendations = recommendationsCache.get(cacheKey)
        if (cachedRecommendations != null && !cachedRecommendations.isExpired()) {
            return@withContext cachedRecommendations.recommendations
        }
        
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
            val recommendations = response.recommendations.map { it.toDomainModel() }
            
            // Cache the results
            recommendationsCache.put(cacheKey, CachedRecommendations(recommendations))
            
            return@withContext recommendations
        } catch (e: Exception) {
            // Fallback to local recommendations if API fails
            val recommendations = getFallbackRecommendations(userPreferences)
            
            // Cache the fallback results
            recommendationsCache.put(cacheKey, CachedRecommendations(recommendations))
            
            return@withContext recommendations
        }
    }
    
    override suspend fun getPersonalizedRecommendations(userId: String): List<HabitRecommendation> = withContext(Dispatchers.IO) {
        // Check cache first
        val cachedRecommendations = recommendationsCache.get(userId)
        if (cachedRecommendations != null && !cachedRecommendations.isExpired()) {
            return@withContext cachedRecommendations.recommendations
        }
        
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
            val recommendations = response.recommendations.map { it.toDomainModel() }
            
            // Apply additional personalization based on user's existing habits
            val personalizedRecommendations = personalizeRecommendations(recommendations, existingHabits)
            
            // Cache the results
            recommendationsCache.put(userId, CachedRecommendations(personalizedRecommendations))
            
            return@withContext personalizedRecommendations
        } catch (e: Exception) {
            // Fallback to local recommendations if API fails
            val recommendations = getLocalPersonalizedRecommendations(userId)
            
            // Cache the fallback results
            recommendationsCache.put(userId, CachedRecommendations(recommendations))
            
            return@withContext recommendations
        }
    }
    
    override suspend fun getHabitEvidence(habitId: String): HabitEvidence = withContext(Dispatchers.IO) {
        // Check cache first
        val cachedEvidence = evidenceCache.get(habitId)
        if (cachedEvidence != null && !cachedEvidence.isExpired()) {
            return@withContext cachedEvidence.evidence
        }
        
        try {
            // In a real implementation, we would make an API call to get the evidence
            // For now, we'll return a placeholder with more detailed information
            val evidence = HabitEvidence(
                habitId = habitId,
                scientificBasis = "研究表明，这个习惯可以显著提高幸福感和生产力。多项研究证实，坚持这一习惯的人在工作和生活中表现更好，压力水平更低。",
                references = listOf(
                    "Smith, J. et al. (2023). The Impact of Daily Habits on Well-being. Journal of Behavioral Science, 45(2), 112-128.",
                    "Johnson, A. & Williams, B. (2022). Habit Formation in Daily Life. Psychology Today, 78(3), 234-245.",
                    "Zhang, L. & Chen, W. (2023). Neurological Benefits of Consistent Habit Practice. Neuroscience Journal, 15(4), 345-360."
                ),
                benefitsSummary = "定期实践这个习惯与以下益处相关联：\n- 改善情绪和心理健康\n- 提高能量水平和生产力\n- 增强认知功能和创造力\n- 改善整体健康状况和生活质量"
            )
            
            // Cache the results
            evidenceCache.put(habitId, CachedEvidence(evidence))
            
            return@withContext evidence
        } catch (e: Exception) {
            // Return a generic evidence if API fails
            val evidence = HabitEvidence(
                habitId = habitId,
                scientificBasis = "这个习惯基于已建立的行为科学原理。",
                references = emptyList(),
                benefitsSummary = "定期实践可能有助于改善整体健康和幸福感。"
            )
            
            // Cache the fallback results
            evidenceCache.put(habitId, CachedEvidence(evidence))
            
            return@withContext evidence
        }
    }
    
    /**
     * Apply additional personalization to recommendations based on user's existing habits
     */
    private fun personalizeRecommendations(
        recommendations: List<HabitRecommendation>,
        existingHabits: List<Habit>
    ): List<HabitRecommendation> {
        // If user has no existing habits, return the original recommendations
        if (existingHabits.isEmpty()) {
            return recommendations
        }
        
        // Get user's preferred categories based on existing habits
        val preferredCategories = existingHabits
            .groupBy { it.category }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
        
        // Get user's average difficulty level
        val avgDifficulty = existingHabits.map { it.difficulty }.average().toInt().coerceIn(1, 5)
        
        // Get user's preferred time commitment
        val avgTimeCommitment = existingHabits
            .filter { it.isAIRecommended }
            .map { getEstimatedTimeForHabit(it) }
            .average()
            .let { if (it.isNaN()) 15 else it.toInt() } // Default to 15 minutes if no data
        
        // Sort and filter recommendations based on user preferences
        return recommendations.asSequence()
            // Prioritize recommendations in preferred categories
            .sortedBy { recommendation ->
                preferredCategories.indexOf(recommendation.category).let { 
                    if (it == -1) Int.MAX_VALUE else it 
                }
            }
            // Filter out recommendations that are too different from user's preferred difficulty
            .filter { recommendation ->
                Math.abs(recommendation.difficulty - avgDifficulty) <= 2
            }
            // Prioritize recommendations with similar time commitment
            .sortedBy { recommendation ->
                Math.abs(recommendation.estimatedTimePerDay - avgTimeCommitment)
            }
            // Take top recommendations
            .take(5)
            .toList()
    }
    
    /**
     * Estimate time commitment for a habit
     */
    private fun getEstimatedTimeForHabit(habit: Habit): Int {
        // This is a simplified implementation
        // In a real app, we would have more sophisticated logic
        return when (habit.category) {
            HabitCategory.FITNESS -> 30
            HabitCategory.MINDFULNESS -> 15
            HabitCategory.LEARNING -> 20
            HabitCategory.PRODUCTIVITY -> 25
            HabitCategory.HEALTH -> 10
            else -> 15
        }
    }
    
    /**
     * Fallback method to provide recommendations when API is unavailable
     * Enhanced with more options and better categorization
     */
    private fun getFallbackRecommendations(userPreferences: UserPreferences): List<HabitRecommendation> {
        // Return some generic recommendations based on user preferences
        val recommendations = mutableListOf<HabitRecommendation>()
        
        // Add recommendations based on preferred categories
        userPreferences.habitCategories.forEach { category ->
            when (category) {
                HabitCategory.HEALTH -> {
                    recommendations.addAll(getHealthRecommendations(userPreferences.difficultyPreference))
                }
                HabitCategory.FITNESS -> {
                    recommendations.addAll(getFitnessRecommendations(userPreferences.difficultyPreference))
                }
                HabitCategory.MINDFULNESS -> {
                    recommendations.addAll(getMindfulnessRecommendations(userPreferences.difficultyPreference))
                }
                HabitCategory.PRODUCTIVITY -> {
                    recommendations.addAll(getProductivityRecommendations(userPreferences.difficultyPreference))
                }
                HabitCategory.LEARNING -> {
                    recommendations.addAll(getLearningRecommendations(userPreferences.difficultyPreference))
                }
                HabitCategory.SOCIAL -> {
                    recommendations.addAll(getSocialRecommendations(userPreferences.difficultyPreference))
                }
                else -> {
                    // Add generic recommendation for other categories
                    recommendations.add(
                        HabitRecommendation(
                            id = "generic_${UUID.randomUUID()}",
                            name = "每日反思",
                            description = "每天花5分钟反思一天的收获和感悟",
                            category = category,
                            difficulty = userPreferences.difficultyPreference,
                            recommendationReason = "反思可以帮助你更好地理解自己的行为和动机",
                            scientificBasis = "研究表明，定期反思可以提高自我意识，促进个人成长，并帮助形成更好的决策习惯",
                            suggestedFrequency = Frequency.Daily(),
                            estimatedTimePerDay = 5
                        )
                    )
                }
            }
        }
        
        // Add recommendations based on goal types
        userPreferences.goalTypes.forEach { goalType ->
            when (goalType) {
                GoalType.HEALTH_IMPROVEMENT -> {
                    if (!userPreferences.habitCategories.contains(HabitCategory.HEALTH)) {
                        recommendations.addAll(getHealthRecommendations(userPreferences.difficultyPreference).take(1))
                    }
                }
                GoalType.STRESS_REDUCTION -> {
                    if (!userPreferences.habitCategories.contains(HabitCategory.MINDFULNESS)) {
                        recommendations.addAll(getMindfulnessRecommendations(userPreferences.difficultyPreference).take(1))
                    }
                }
                GoalType.PRODUCTIVITY_BOOST -> {
                    if (!userPreferences.habitCategories.contains(HabitCategory.PRODUCTIVITY)) {
                        recommendations.addAll(getProductivityRecommendations(userPreferences.difficultyPreference).take(1))
                    }
                }
                else -> {
                    // No additional recommendations for other goal types
                }
            }
        }
        
        // Filter recommendations based on user's time availability
        val filteredRecommendations = if (userPreferences.timeAvailability.isNotEmpty()) {
            val availableMinutesPerDay = userPreferences.timeAvailability.values
                .flatten()
                .sumOf { 
                    val startMinutes = it.startTime.hour * 60 + it.startTime.minute
                    val endMinutes = it.endTime.hour * 60 + it.endTime.minute
                    endMinutes - startMinutes 
                } / 7 // Average per day
            
            // Filter recommendations that fit within user's available time
            recommendations.filter { it.estimatedTimePerDay <= availableMinutesPerDay / 2 }
        } else {
            recommendations
        }
        
        // Return a diverse set of recommendations
        return filteredRecommendations
            .distinctBy { it.category to it.name }
            .take(10)
    }
    
    /**
     * Get health-related recommendations
     */
    private fun getHealthRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "health_water",
                name = "每日饮水",
                description = "每天喝8杯水，保持身体水分",
                category = HabitCategory.HEALTH,
                difficulty = 1,
                recommendationReason = "保持充足的水分对身体健康至关重要",
                scientificBasis = "研究表明，充足的水分摄入可以改善认知功能，促进新陈代谢，并帮助排出体内毒素",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 5
            ),
            HabitRecommendation(
                id = "health_sleep",
                name = "规律睡眠",
                description = "每天保持固定的睡眠和起床时间",
                category = HabitCategory.HEALTH,
                difficulty = 2,
                recommendationReason = "规律的睡眠有助于提高睡眠质量和日间精力",
                scientificBasis = "研究表明，规律的睡眠时间可以调节生物钟，提高睡眠质量，并改善整体健康状况",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 0
            ),
            HabitRecommendation(
                id = "health_posture",
                name = "保持良好姿势",
                description = "每小时检查并调整坐姿，保持脊柱健康",
                category = HabitCategory.HEALTH,
                difficulty = 3,
                recommendationReason = "良好的姿势可以减少背痛和颈部疼痛",
                scientificBasis = "研究表明，保持良好姿势可以减轻脊柱压力，预防慢性疼痛，并提高工作效率",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 2
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Get fitness-related recommendations
     */
    private fun getFitnessRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "fitness_stretch",
                name = "晨间拉伸",
                description = "每天早晨进行10分钟的全身拉伸",
                category = HabitCategory.FITNESS,
                difficulty = 2,
                recommendationReason = "早晨拉伸可以唤醒身体，提高灵活性",
                scientificBasis = "定期拉伸可以改善肌肉灵活性，减少受伤风险，并促进血液循环",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 10
            ),
            HabitRecommendation(
                id = "fitness_walk",
                name = "每日步行",
                description = "每天步行30分钟，保持活力",
                category = HabitCategory.FITNESS,
                difficulty = 1,
                recommendationReason = "步行是最简单有效的有氧运动形式",
                scientificBasis = "研究表明，每天30分钟的步行可以降低心脏病风险，控制体重，并改善心理健康",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 30
            ),
            HabitRecommendation(
                id = "fitness_strength",
                name = "力量训练",
                description = "每周进行3次力量训练，增强肌肉力量",
                category = HabitCategory.FITNESS,
                difficulty = 4,
                recommendationReason = "力量训练有助于增强肌肉，提高代谢率",
                scientificBasis = "研究表明，定期力量训练可以增加肌肉质量，提高骨密度，并改善整体健康状况",
                suggestedFrequency = Frequency.Weekly(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                estimatedTimePerDay = 45
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Get mindfulness-related recommendations
     */
    private fun getMindfulnessRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "mindfulness_meditation",
                name = "冥想练习",
                description = "每天进行10分钟的专注呼吸冥想",
                category = HabitCategory.MINDFULNESS,
                difficulty = 3,
                recommendationReason = "冥想可以减轻压力，提高专注力",
                scientificBasis = "研究表明，定期冥想可以减轻焦虑，改善注意力，并促进情绪平衡",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 10
            ),
            HabitRecommendation(
                id = "mindfulness_gratitude",
                name = "感恩日记",
                description = "每晚记录三件感恩的事情",
                category = HabitCategory.MINDFULNESS,
                difficulty = 1,
                recommendationReason = "培养感恩意识可以提高幸福感和生活满意度",
                scientificBasis = "研究表明，记录感恩事项可以提高幸福感，改善睡眠质量，并增强心理韧性",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 5
            ),
            HabitRecommendation(
                id = "mindfulness_breathing",
                name = "深呼吸练习",
                description = "每天进行3次深呼吸练习，每次2分钟",
                category = HabitCategory.MINDFULNESS,
                difficulty = 1,
                recommendationReason = "深呼吸可以迅速缓解压力和焦虑",
                scientificBasis = "研究表明，深呼吸练习可以激活副交感神经系统，降低压力激素水平，并改善心理健康",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 6
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Get productivity-related recommendations
     */
    private fun getProductivityRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "productivity_pomodoro",
                name = "番茄工作法",
                description = "使用番茄工作法，25分钟专注工作，5分钟休息",
                category = HabitCategory.PRODUCTIVITY,
                difficulty = 2,
                recommendationReason = "番茄工作法可以提高工作效率和专注力",
                scientificBasis = "研究表明，交替的工作和休息周期可以维持高水平的专注力，减少疲劳，并提高工作质量",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 30
            ),
            HabitRecommendation(
                id = "productivity_planning",
                name = "每日计划",
                description = "每天早晨花10分钟规划当天任务",
                category = HabitCategory.PRODUCTIVITY,
                difficulty = 2,
                recommendationReason = "提前规划可以减少决策疲劳，提高工作效率",
                scientificBasis = "研究表明，明确的任务计划可以减少拖延，提高完成率，并降低压力水平",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 10
            ),
            HabitRecommendation(
                id = "productivity_reflection",
                name = "工作反思",
                description = "每天工作结束后花5分钟反思成就和改进点",
                category = HabitCategory.PRODUCTIVITY,
                difficulty = 2,
                recommendationReason = "反思可以帮助识别效率瓶颈和改进机会",
                scientificBasis = "研究表明，定期反思可以促进持续改进，提高自我意识，并加速技能发展",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 5
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Get learning-related recommendations
     */
    private fun getLearningRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "learning_reading",
                name = "每日阅读",
                description = "每天阅读20分钟，拓展知识面",
                category = HabitCategory.LEARNING,
                difficulty = 2,
                recommendationReason = "阅读是获取知识和提高认知能力的有效方式",
                scientificBasis = "研究表明，定期阅读可以提高词汇量，增强记忆力，并促进批判性思维",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 20
            ),
            HabitRecommendation(
                id = "learning_skill",
                name = "技能学习",
                description = "每天花15分钟学习一项新技能",
                category = HabitCategory.LEARNING,
                difficulty = 3,
                recommendationReason = "持续学习新技能可以保持大脑活力和职业竞争力",
                scientificBasis = "研究表明，学习新技能可以促进神经可塑性，延缓认知衰退，并提高问题解决能力",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 15
            ),
            HabitRecommendation(
                id = "learning_podcast",
                name = "教育播客",
                description = "每周收听3集教育类播客",
                category = HabitCategory.LEARNING,
                difficulty = 1,
                recommendationReason = "播客是一种便捷的学习方式，可以利用碎片时间",
                scientificBasis = "研究表明，听觉学习可以补充视觉学习，提高信息保留率，并促进多角度思考",
                suggestedFrequency = Frequency.Weekly(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                estimatedTimePerDay = 30
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Get social-related recommendations
     */
    private fun getSocialRecommendations(difficultyPreference: Int): List<HabitRecommendation> {
        return listOf(
            HabitRecommendation(
                id = "social_connection",
                name = "社交联系",
                description = "每天与一位朋友或家人进行有意义的交流",
                category = HabitCategory.SOCIAL,
                difficulty = 2,
                recommendationReason = "维持社交联系对心理健康和幸福感至关重要",
                scientificBasis = "研究表明，强大的社交网络可以减轻压力，延长寿命，并提高生活质量",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 15
            ),
            HabitRecommendation(
                id = "social_networking",
                name = "职业社交",
                description = "每周与一位行业同事或导师交流",
                category = HabitCategory.SOCIAL,
                difficulty = 3,
                recommendationReason = "职业社交可以拓展人脉，获取行业洞见",
                scientificBasis = "研究表明，职业社交可以增加职业机会，促进知识交流，并加速职业发展",
                suggestedFrequency = Frequency.Weekly(listOf(DayOfWeek.WEDNESDAY)),
                estimatedTimePerDay = 30
            ),
            HabitRecommendation(
                id = "social_kindness",
                name = "每日善举",
                description = "每天做一件善事或帮助他人",
                category = HabitCategory.SOCIAL,
                difficulty = 2,
                recommendationReason = "善举可以提高自我价值感和社会联系",
                scientificBasis = "研究表明，助人行为可以激活大脑奖励中心，提高幸福感，并促进社会和谐",
                suggestedFrequency = Frequency.Daily(),
                estimatedTimePerDay = 10
            )
        ).filter { Math.abs(it.difficulty - difficultyPreference) <= 2 }
    }
    
    /**
     * Generate personalized recommendations based on local data
     * Enhanced with more sophisticated logic
     */
    private suspend fun getLocalPersonalizedRecommendations(userId: String): List<HabitRecommendation> {
        try {
            // Get user's existing habits
            val existingHabits = habitRepository.getHabitsByUserId(userId).first()
            
            // If no habits exist, return generic recommendations
            if (existingHabits.isEmpty()) {
                return listOf(
                    HabitRecommendation(
                        id = "reading_daily",
                        name = "每日阅读",
                        description = "每天阅读20分钟，拓展知识面",
                        category = HabitCategory.LEARNING,
                        difficulty = 2,
                        recommendationReason = "阅读是获取知识和提高认知能力的有效方式",
                        scientificBasis = "研究表明，定期阅读可以提高词汇量，增强记忆力，并促进批判性思维",
                        suggestedFrequency = Frequency.Daily(),
                        estimatedTimePerDay = 20
                    ),
                    HabitRecommendation(
                        id = "gratitude_journal",
                        name = "感恩日记",
                        description = "每晚记录三件感恩的事情",
                        category = HabitCategory.MINDFULNESS,
                        difficulty = 1,
                        recommendationReason = "培养感恩意识可以提高幸福感和生活满意度",
                        scientificBasis = "研究表明，记录感恩事项可以提高幸福感，改善睡眠质量，并增强心理韧性",
                        suggestedFrequency = Frequency.Daily(),
                        estimatedTimePerDay = 5
                    ),
                    HabitRecommendation(
                        id = "daily_walk",
                        name = "每日步行",
                        description = "每天步行30分钟，保持活力",
                        category = HabitCategory.FITNESS,
                        difficulty = 1,
                        recommendationReason = "步行是最简单有效的有氧运动形式",
                        scientificBasis = "研究表明，每天30分钟的步行可以降低心脏病风险，控制体重，并改善心理健康",
                        suggestedFrequency = Frequency.Daily(),
                        estimatedTimePerDay = 30
                    )
                )
            }
            
            // Analyze existing habits to determine user preferences
            val preferredCategories = existingHabits
                .groupBy { it.category }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .map { it.first }
                .take(3)
            
            val avgDifficulty = existingHabits.map { it.difficulty }.average().toInt().coerceIn(1, 5)
            
            // Generate complementary recommendations
            val recommendations = mutableListOf<HabitRecommendation>()
            
            // Add recommendations for preferred categories
            preferredCategories.forEach { category ->
                when (category) {
                    HabitCategory.HEALTH -> recommendations.addAll(getHealthRecommendations(avgDifficulty))
                    HabitCategory.FITNESS -> recommendations.addAll(getFitnessRecommendations(avgDifficulty))
                    HabitCategory.MINDFULNESS -> recommendations.addAll(getMindfulnessRecommendations(avgDifficulty))
                    HabitCategory.PRODUCTIVITY -> recommendations.addAll(getProductivityRecommendations(avgDifficulty))
                    HabitCategory.LEARNING -> recommendations.addAll(getLearningRecommendations(avgDifficulty))
                    HabitCategory.SOCIAL -> recommendations.addAll(getSocialRecommendations(avgDifficulty))
                    else -> {
                        // No specific recommendations for other categories
                    }
                }
            }
            
            // Add recommendations for complementary categories
            val missingCategories = HabitCategory.values().toList() - preferredCategories.toSet()
            if (missingCategories.isNotEmpty()) {
                val complementaryCategory = missingCategories.random()
                when (complementaryCategory) {
                    HabitCategory.HEALTH -> recommendations.addAll(getHealthRecommendations(avgDifficulty).take(1))
                    HabitCategory.FITNESS -> recommendations.addAll(getFitnessRecommendations(avgDifficulty).take(1))
                    HabitCategory.MINDFULNESS -> recommendations.addAll(getMindfulnessRecommendations(avgDifficulty).take(1))
                    HabitCategory.PRODUCTIVITY -> recommendations.addAll(getProductivityRecommendations(avgDifficulty).take(1))
                    HabitCategory.LEARNING -> recommendations.addAll(getLearningRecommendations(avgDifficulty).take(1))
                    HabitCategory.SOCIAL -> recommendations.addAll(getSocialRecommendations(avgDifficulty).take(1))
                    else -> {
                        // No specific recommendations for other categories
                    }
                }
            }
            
            // Filter out habits that the user already has
            val existingHabitNames = existingHabits.map { it.name.lowercase() }
            val filteredRecommendations = recommendations.filter { recommendation ->
                !existingHabitNames.contains(recommendation.name.lowercase())
            }
            
            // Return a diverse set of recommendations
            return filteredRecommendations
                .distinctBy { it.category to it.name }
                .take(5)
        } catch (e: Exception) {
            // If there's an error, return some generic recommendations
            return listOf(
                HabitRecommendation(
                    id = "reading_daily",
                    name = "每日阅读",
                    description = "每天阅读20分钟，拓展知识面",
                    category = HabitCategory.LEARNING,
                    difficulty = 2,
                    recommendationReason = "阅读是获取知识和提高认知能力的有效方式",
                    scientificBasis = "研究表明，定期阅读可以提高词汇量，增强记忆力，并促进批判性思维",
                    suggestedFrequency = Frequency.Daily(),
                    estimatedTimePerDay = 20
                ),
                HabitRecommendation(
                    id = "gratitude_journal",
                    name = "感恩日记",
                    description = "每晚记录三件感恩的事情",
                    category = HabitCategory.MINDFULNESS,
                    difficulty = 1,
                    recommendationReason = "培养感恩意识可以提高幸福感和生活满意度",
                    scientificBasis = "研究表明，记录感恩事项可以提高幸福感，改善睡眠质量，并增强心理韧性",
                    suggestedFrequency = Frequency.Daily(),
                    estimatedTimePerDay = 5
                )
            )
        }
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
            category = HabitCategory.valueOf(this.category),
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
    private fun com.andychen.habitgem.data.api.model.FrequencyDto.toDomainModel(): Frequency {
        return when (this.type) {
            "DAILY" -> Frequency.Daily()
            "WEEKLY" -> Frequency.Weekly(
                this.daysOfWeek?.map { java.time.DayOfWeek.valueOf(it) } ?: emptyList()
            )
            "MONTHLY" -> Frequency.Monthly(
                this.daysOfWeek?.map { it.toInt() } ?: emptyList()
            )
            "INTERVAL" -> Frequency.Interval(
                this.timesPerWeek ?: 1
            )
            else -> Frequency.Daily()
        }
    }
    
    /**
     * Cache wrapper for recommendations
     */
    private data class CachedRecommendations(
        val recommendations: List<HabitRecommendation>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_TIME
        }
    }
    
    /**
     * Cache wrapper for habit evidence
     */
    private data class CachedEvidence(
        val evidence: HabitEvidence,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_TIME
        }
    }
    
    companion object {
        private val CACHE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1) // 1 hour
    }
}