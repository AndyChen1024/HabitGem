package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.AIAssistantRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.ActionType
import com.andychen.habitgem.domain.model.AssistantAction
import com.andychen.habitgem.domain.model.AssistantResponse
import com.andychen.habitgem.domain.model.ResponseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Implementation of AIAssistantService
 */
class AIAssistantServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : AIAssistantService {
    
    override suspend fun sendMessage(
        userId: String,
        message: String,
        conversationId: String?
    ): AssistantResponse = withContext(Dispatchers.IO) {
        try {
            // Make API request
            val response = aiServiceApi.getAssistantResponse(
                AIAssistantRequest(
                    userId = userId,
                    message = message,
                    context = null, // No specific context
                    conversationId = conversationId
                )
            )
            
            // Convert to domain model
            return@withContext AssistantResponse(
                message = response.message,
                type = ResponseType.valueOf(response.type),
                relatedHabits = response.relatedHabits,
                actionSuggestions = response.actionSuggestions?.map {
                    AssistantAction(
                        type = ActionType.valueOf(it.type),
                        title = it.title,
                        payload = it.payload
                    )
                }
            )
        } catch (e: Exception) {
            // Fallback to local response if API fails
            return@withContext generateLocalResponse(message)
        }
    }
    
    override suspend fun getContextualSuggestions(
        userId: String,
        context: AssistantContext
    ): List<AssistantAction> = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, we would make an API call to get contextual suggestions
            // For now, we'll generate some sample suggestions based on the context
            return@withContext when (context) {
                AssistantContext.HABIT_CREATION -> getHabitCreationSuggestions(userId)
                AssistantContext.HABIT_COMPLETION -> getHabitCompletionSuggestions(userId)
                AssistantContext.MISSED_HABIT -> getMissedHabitSuggestions(userId)
                AssistantContext.PROGRESS_REVIEW -> getProgressReviewSuggestions(userId)
                AssistantContext.GENERAL -> getGeneralSuggestions(userId)
            }
        } catch (e: Exception) {
            // Return empty list if there's an error
            return@withContext emptyList()
        }
    }
    
    /**
     * Generate a local response based on the user's message
     */
    private fun generateLocalResponse(message: String): AssistantResponse {
        // Simple keyword-based response generation
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("推荐") && lowerMessage.contains("习惯") -> {
                AssistantResponse(
                    message = "我可以根据您的目标和偏好推荐适合的习惯。您对哪个领域的习惯感兴趣？健康、学习、生产力还是其他？",
                    type = ResponseType.SUGGESTION,
                    relatedHabits = null,
                    actionSuggestions = listOf(
                        AssistantAction(
                            type = ActionType.CREATE_HABIT,
                            title = "创建新习惯",
                            payload = mapOf("category" to "HEALTH")
                        )
                    )
                )
            }
            lowerMessage.contains("分析") || lowerMessage.contains("统计") -> {
                AssistantResponse(
                    message = "我可以帮您分析习惯数据，查看完成率、趋势和模式。您想了解哪个习惯的分析？",
                    type = ResponseType.ANALYSIS,
                    relatedHabits = null,
                    actionSuggestions = listOf(
                        AssistantAction(
                            type = ActionType.VIEW_ANALYSIS,
                            title = "查看习惯分析",
                            payload = null
                        )
                    )
                )
            }
            lowerMessage.contains("坚持") || lowerMessage.contains("动力") -> {
                AssistantResponse(
                    message = "坚持习惯确实需要一些策略。研究表明，将新习惯与现有习惯关联、设置明确的触发因素，以及庆祝小成就都有助于建立持久的习惯。您想了解更多具体的策略吗？",
                    type = ResponseType.TEXT,
                    relatedHabits = null,
                    actionSuggestions = null
                )
            }
            else -> {
                AssistantResponse(
                    message = "我是您的AI习惯助手，可以帮您推荐习惯、分析进度、提供建议和回答问题。请告诉我您需要什么帮助？",
                    type = ResponseType.TEXT,
                    relatedHabits = null,
                    actionSuggestions = null
                )
            }
        }
    }
    
    /**
     * Get suggestions for habit creation context
     */
    private suspend fun getHabitCreationSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "创建健康习惯",
                payload = mapOf("category" to "HEALTH")
            ),
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "创建学习习惯",
                payload = mapOf("category" to "LEARNING")
            ),
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "创建冥想习惯",
                payload = mapOf("category" to "MINDFULNESS")
            )
        )
    }
    
    /**
     * Get suggestions for habit completion context
     */
    private suspend fun getHabitCompletionSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.VIEW_ANALYSIS,
                title = "查看习惯进度",
                payload = null
            ),
            AssistantAction(
                type = ActionType.EXTERNAL_LINK,
                title = "了解习惯科学",
                payload = mapOf("url" to "https://habitgem.com/science")
            )
        )
    }
    
    /**
     * Get suggestions for missed habit context
     */
    private suspend fun getMissedHabitSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.MODIFY_HABIT,
                title = "调整习惯难度",
                payload = null
            ),
            AssistantAction(
                type = ActionType.MODIFY_HABIT,
                title = "更改习惯时间",
                payload = null
            )
        )
    }
    
    /**
     * Get suggestions for progress review context
     */
    private suspend fun getProgressReviewSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.VIEW_ANALYSIS,
                title = "查看详细分析",
                payload = null
            ),
            AssistantAction(
                type = ActionType.VIEW_HABIT,
                title = "查看习惯详情",
                payload = null
            )
        )
    }
    
    /**
     * Get general suggestions
     */
    private suspend fun getGeneralSuggestions(userId: String): List<AssistantAction> {
        // Get user's habits
        val habits = habitRepository.getHabitsByUserId(userId).first()
        
        val suggestions = mutableListOf<AssistantAction>()
        
        // Add general suggestions
        suggestions.add(
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "创建新习惯",
                payload = null
            )
        )
        
        // Add habit-specific suggestions if user has habits
        if (habits.isNotEmpty()) {
            suggestions.add(
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看习惯分析",
                    payload = null
                )
            )
        }
        
        return suggestions
    }
}