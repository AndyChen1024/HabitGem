package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.domain.model.AssistantAction
import com.andychen.habitgem.domain.model.AssistantResponse

/**
 * Service for AI assistant interactions
 */
interface AIAssistantService {
    /**
     * Send a message to the AI assistant and get a response
     * @param userId User ID
     * @param message User's message
     * @param conversationId Optional conversation ID for continuing a conversation
     * @return Assistant's response
     */
    suspend fun sendMessage(
        userId: String,
        message: String,
        conversationId: String? = null
    ): AssistantResponse
    
    /**
     * Get contextual suggestions based on the current context
     * @param userId User ID
     * @param context Context type (e.g., HABIT_CREATION, HABIT_COMPLETION)
     * @return List of suggested actions
     */
    suspend fun getContextualSuggestions(
        userId: String,
        context: AssistantContext
    ): List<AssistantAction>
}

/**
 * Enum for assistant context types
 */
enum class AssistantContext {
    HABIT_CREATION,
    HABIT_COMPLETION,
    MISSED_HABIT,
    PROGRESS_REVIEW,
    GENERAL
}