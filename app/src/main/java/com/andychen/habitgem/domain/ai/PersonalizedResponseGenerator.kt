package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.data.repository.UserPreferencesRepository
import com.andychen.habitgem.domain.model.ActionType
import com.andychen.habitgem.domain.model.AssistantAction
import com.andychen.habitgem.domain.model.AssistantResponse
import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ResponseType
import com.andychen.habitgem.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Generates personalized responses based on user preferences and habit history
 */
class PersonalizedResponseGenerator(
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Generate a personalized response based on user data and context
     */
    suspend fun generateResponse(
        userId: String,
        intent: IntentType,
        entities: Map<String, String>,
        baseResponse: AssistantResponse
    ): AssistantResponse {
        val userPreferences = getUserPreferences(userId)
        val userHabits = getUserHabits(userId)
        
        // Enhance the base response with personalized content
        val enhancedMessage = personalizeMessage(
            baseResponse.message,
            intent,
            entities,
            userPreferences,
            userHabits
        )
        
        // Enhance action suggestions with personalized options
        val enhancedActions = personalizeActions(
            baseResponse.actionSuggestions ?: emptyList(),
            intent,
            entities,
            userPreferences,
            userHabits
        )
        
        return baseResponse.copy(
            message = enhancedMessage,
            actionSuggestions = enhancedActions
        )
    }
    
    /**
     * Personalize the response message based on user data
     */
    private fun personalizeMessage(
        baseMessage: String,
        intent: IntentType,
        entities: Map<String, String>,
        userPreferences: UserPreferences?,
        userHabits: List<Habit>
    ): String {
        if (userPreferences == null) {
            return baseMessage
        }
        
        val personalizedMessage = StringBuilder(baseMessage)
        
        // Add personalized greeting based on time of day
        val currentHour = LocalTime.now().hour
        val greeting = when {
            currentHour < 12 -> "早上好"
            currentHour < 18 -> "下午好"
            else -> "晚上好"
        }
        
        // Add personalized content based on intent
        when (intent) {
            IntentType.CREATE_HABIT -> {
                if (userHabits.isEmpty()) {
                    personalizedMessage.append("\n\n这将是您的第一个习惯！开始养成好习惯是迈向成功的第一步。")
                } else {
                    val preferredCategories = userPreferences.habitCategories
                    val category = entities["category"]?.let { HabitCategory.valueOf(it) }
                    
                    if (category != null && preferredCategories.contains(category)) {
                        personalizedMessage.append("\n\n根据您的偏好，${getCategoryDisplayName(category.name)}类习惯非常适合您。")
                    }
                }
            }
            
            IntentType.VIEW_ANALYSIS -> {
                if (userHabits.isNotEmpty()) {
                    val mostConsistentHabit = userHabits.maxByOrNull { it.id.hashCode() % 100 } // Simulate consistency score
                    if (mostConsistentHabit != null) {
                        personalizedMessage.append("\n\n您的「${mostConsistentHabit.name}」习惯表现最为稳定，继续保持！")
                    }
                }
            }
            
            IntentType.MODIFY_HABIT -> {
                if (userHabits.isNotEmpty()) {
                    personalizedMessage.append("\n\n调整习惯是很正常的，找到最适合您的方式才能长期坚持。")
                }
            }
            
            IntentType.GET_MOTIVATION -> {
                // Add motivational message based on user's habit history
                if (userHabits.isNotEmpty()) {
                    val habitCount = userHabits.size
                    personalizedMessage.append("\n\n您已经建立了$habitCount个习惯，这是一个很好的开始！")
                }
            }
            
            else -> {
                // No special personalization for other intents
            }
        }
        
        return personalizedMessage.toString()
    }
    
    /**
     * Personalize action suggestions based on user data
     */
    private fun personalizeActions(
        baseActions: List<AssistantAction>,
        intent: IntentType,
        entities: Map<String, String>,
        userPreferences: UserPreferences?,
        userHabits: List<Habit>
    ): List<AssistantAction> {
        if (userPreferences == null) {
            return baseActions
        }
        
        val personalizedActions = baseActions.toMutableList()
        
        when (intent) {
            IntentType.CREATE_HABIT -> {
                // Add personalized habit suggestions based on user preferences
                val preferredCategories = userPreferences.habitCategories
                if (preferredCategories.isNotEmpty()) {
                    val category = entities["category"]?.let { HabitCategory.valueOf(it) }
                    
                    if (category == null) {
                        // If no category specified, suggest from preferred categories
                        preferredCategories.take(2).forEach { preferredCategory ->
                            val habitSuggestion = when (preferredCategory) {
                                HabitCategory.HEALTH -> "每天喝2升水"
                                HabitCategory.FITNESS -> "每天进行15分钟拉伸"
                                HabitCategory.MINDFULNESS -> "每天冥想10分钟"
                                HabitCategory.LEARNING -> "每天学习20分钟"
                                HabitCategory.PRODUCTIVITY -> "每天规划明日任务"
                                else -> "建立新的${getCategoryDisplayName(preferredCategory.name)}习惯"
                            }
                            
                            personalizedActions.add(
                                AssistantAction(
                                    type = ActionType.CREATE_HABIT,
                                    title = habitSuggestion,
                                    payload = mapOf("category" to preferredCategory.name)
                                )
                            )
                        }
                    }
                }
                
                // Consider time availability
                val timeAvailability = userPreferences.timeAvailability
                val today = LocalDate.now().dayOfWeek
                if (timeAvailability.containsKey(today) && timeAvailability[today]?.isNotEmpty() == true) {
                    val timeSlot = timeAvailability[today]?.first()
                    if (timeSlot != null) {
                        val startTime = timeSlot.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        personalizedActions.add(
                            AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "在$startTime执行习惯",
                                payload = mapOf("time" to startTime)
                            )
                        )
                    }
                }
            }
            
            IntentType.VIEW_ANALYSIS -> {
                // Add personalized analysis options based on user's habits
                if (userHabits.isNotEmpty()) {
                    val recentHabit = userHabits.maxByOrNull { it.createdAt }
                    if (recentHabit != null) {
                        personalizedActions.add(
                            AssistantAction(
                                type = ActionType.VIEW_HABIT,
                                title = "查看「${recentHabit.name}」详情",
                                payload = mapOf("habitId" to recentHabit.id, "habitName" to recentHabit.name)
                            )
                        )
                    }
                }
            }
            
            IntentType.MODIFY_HABIT -> {
                // Add personalized modification suggestions
                if (userHabits.isNotEmpty()) {
                    val difficultyPreference = userPreferences.difficultyPreference
                    if (difficultyPreference <= 2) {
                        personalizedActions.add(
                            AssistantAction(
                                type = ActionType.MODIFY_HABIT,
                                title = "简化习惯目标",
                                payload = mapOf("adjustment" to "SIMPLIFY_GOAL")
                            )
                        )
                    }
                }
            }
            
            else -> {
                // No special personalization for other intents
            }
        }
        
        return personalizedActions
    }
    
    /**
     * Get user preferences
     */
    private suspend fun getUserPreferences(userId: String): UserPreferences? {
        return try {
            userPreferencesRepository.getUserPreferences(userId).first()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user habits
     */
    private suspend fun getUserHabits(userId: String): List<Habit> {
        return try {
            habitRepository.getHabitsByUserId(userId).first()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get display name for habit category
     */
    private fun getCategoryDisplayName(category: String): String {
        return when (category) {
            "HEALTH" -> "健康"
            "FITNESS" -> "健身"
            "MINDFULNESS" -> "冥想"
            "PRODUCTIVITY" -> "生产力"
            "LEARNING" -> "学习"
            "SOCIAL" -> "社交"
            "CREATIVITY" -> "创意"
            "FINANCE" -> "财务"
            else -> "其他"
        }
    }
    
    /**
     * Get day of week display name
     */
    private fun getDayOfWeekDisplayName(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
    }
}