package com.andychen.habitgem.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andychen.habitgem.domain.ai.AIAssistantService
import com.andychen.habitgem.domain.ai.AssistantContext
import com.andychen.habitgem.domain.ai.HabitAnalysisService
import com.andychen.habitgem.domain.model.ActionType
import com.andychen.habitgem.domain.model.AssistantAction
import com.andychen.habitgem.domain.model.AssistantResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the AI Assistant screen
 */
@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val aiAssistantService: AIAssistantService,
    private val habitAnalysisService: HabitAnalysisService
) : ViewModel() {

    // State for the chat messages
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Success)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // State for the input field
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // State for suggested actions
    private val _suggestedActions = MutableStateFlow<List<AssistantAction>>(emptyList())
    val suggestedActions: StateFlow<List<AssistantAction>> = _suggestedActions.asStateFlow()

    // Current conversation ID
    private var conversationId: String? = null

    // Current context
    private var currentContext = AssistantContext.GENERAL

    // Temporary user ID (in a real app, this would come from user authentication)
    private val userId = "user_123"

    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Load suggested actions for initial state
        loadSuggestedActions()
    }

    /**
     * Update the input text
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /**
     * Send a message to the AI assistant
     */
    fun sendMessage() {
        val messageText = _inputText.value.trim()
        if (messageText.isEmpty()) return

        // Add user message to the chat
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = messageText,
                sender = MessageSender.USER,
                timestamp = LocalDateTime.now(),
                actions = emptyList()
            )
        )

        // Clear input field
        _inputText.value = ""

        // Show loading state
        _chatState.value = ChatState.Loading

        // Send message to AI service
        viewModelScope.launch {
            try {
                // Add a small delay to simulate network latency and thinking time
                delay(800)
                
                val response = aiAssistantService.sendMessage(
                    userId = userId,
                    message = messageText,
                    conversationId = conversationId
                )

                // Add assistant response to the chat
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response.message,
                        sender = MessageSender.ASSISTANT,
                        timestamp = LocalDateTime.now(),
                        actions = response.actionSuggestions ?: emptyList()
                    )
                )

                // Update conversation ID if this is the first message
                if (conversationId == null) {
                    conversationId = UUID.randomUUID().toString()
                }

                // Update context based on message content
                updateContextBasedOnMessage(messageText)
                
                // Refresh suggested actions based on new context
                loadSuggestedActions()

                // Update state to success
                _chatState.value = ChatState.Success
            } catch (e: Exception) {
                // Handle error
                _chatState.value = ChatState.Error("无法连接到AI助手，请稍后再试。")
                
                // Fallback to local response
                addMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "抱歉，我暂时无法连接到服务器。我可以帮你回答一些基本问题，或者你可以稍后再试。",
                        sender = MessageSender.ASSISTANT,
                        timestamp = LocalDateTime.now(),
                        actions = getDefaultActions()
                    )
                )
                
                _chatState.value = ChatState.Success
            }
        }
    }

    /**
     * Send a quick question to the AI assistant
     */
    fun sendQuickQuestion(question: String) {
        _inputText.value = question
        sendMessage()
    }

    /**
     * Execute an assistant action
     */
    fun executeAction(action: AssistantAction) {
        // Add a system message acknowledging the action
        addMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "正在执行操作: ${action.title}",
                sender = MessageSender.SYSTEM,
                timestamp = LocalDateTime.now(),
                actions = emptyList()
            )
        )
        
        // Handle different action types
        viewModelScope.launch {
            when (action.type) {
                ActionType.CREATE_HABIT -> {
                    // In a real app, this would navigate to habit creation
                    // For now, just add a response message
                    delay(500) // Small delay for UI feedback
                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "让我帮你创建一个新习惯。请告诉我你想要养成什么样的习惯？",
                            sender = MessageSender.ASSISTANT,
                            timestamp = LocalDateTime.now(),
                            actions = listOf(
                                AssistantAction(
                                    type = ActionType.CREATE_HABIT,
                                    title = "晨间冥想",
                                    payload = mapOf("category" to "MINDFULNESS")
                                ),
                                AssistantAction(
                                    type = ActionType.CREATE_HABIT,
                                    title = "每日阅读",
                                    payload = mapOf("category" to "LEARNING")
                                ),
                                AssistantAction(
                                    type = ActionType.CREATE_HABIT,
                                    title = "喝水提醒",
                                    payload = mapOf("category" to "HEALTH")
                                )
                            )
                        )
                    )
                    currentContext = AssistantContext.HABIT_CREATION
                }
                ActionType.VIEW_ANALYSIS -> {
                    // In a real app, this would navigate to analysis screen
                    // For now, just add a response message
                    delay(500) // Small delay for UI feedback
                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "以下是你的习惯分析概览。你的整体完成率为78%，比上周提高了5%。最近连续坚持了7天，做得很棒！",
                            sender = MessageSender.ASSISTANT,
                            timestamp = LocalDateTime.now(),
                            actions = listOf(
                                AssistantAction(
                                    type = ActionType.VIEW_HABIT,
                                    title = "查看详细分析",
                                    payload = null
                                )
                            )
                        )
                    )
                    currentContext = AssistantContext.PROGRESS_REVIEW
                }
                ActionType.MODIFY_HABIT -> {
                    // In a real app, this would navigate to habit editing
                    // For now, just add a response message
                    delay(500) // Small delay for UI feedback
                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "你想要调整哪个习惯？我可以帮你修改难度、频率或提醒时间。",
                            sender = MessageSender.ASSISTANT,
                            timestamp = LocalDateTime.now(),
                            actions = listOf(
                                AssistantAction(
                                    type = ActionType.MODIFY_HABIT,
                                    title = "降低难度",
                                    payload = mapOf("adjustment" to "DECREASE_DIFFICULTY")
                                ),
                                AssistantAction(
                                    type = ActionType.MODIFY_HABIT,
                                    title = "调整时间",
                                    payload = mapOf("adjustment" to "CHANGE_TIME")
                                ),
                                AssistantAction(
                                    type = ActionType.MODIFY_HABIT,
                                    title = "减少频率",
                                    payload = mapOf("adjustment" to "DECREASE_FREQUENCY")
                                )
                            )
                        )
                    )
                }
                ActionType.VIEW_HABIT -> {
                    // In a real app, this would navigate to habit details
                    // For now, just add a response message
                    delay(500) // Small delay for UI feedback
                    
                    // Get habit name from payload if available
                    val habitName = action.payload?.get("habitName") ?: "习惯"
                    
                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "这是关于「$habitName」的详细信息和统计数据。你已经坚持了23天，完成率为85%。在周二和周四的完成率最高，周末较低。",
                            sender = MessageSender.ASSISTANT,
                            timestamp = LocalDateTime.now(),
                            actions = listOf(
                                AssistantAction(
                                    type = ActionType.MODIFY_HABIT,
                                    title = "调整这个习惯",
                                    payload = mapOf("habitId" to "123")
                                )
                            )
                        )
                    )
                }
                ActionType.EXTERNAL_LINK -> {
                    // In a real app, this would open an external link
                    // For now, just add a response message
                    val url = action.payload?.get("url") ?: "https://habitgem.com"
                    
                    addMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = "正在打开外部链接: $url",
                            sender = MessageSender.SYSTEM,
                            timestamp = LocalDateTime.now(),
                            actions = emptyList()
                        )
                    )
                }
            }
            
            // Refresh suggested actions based on the new context
            loadSuggestedActions()
        }
    }

    /**
     * Update context based on message content
     */
    private fun updateContextBasedOnMessage(message: String) {
        val lowerMessage = message.lowercase()
        
        currentContext = when {
            lowerMessage.contains("创建") || lowerMessage.contains("新习惯") || lowerMessage.contains("推荐") -> 
                AssistantContext.HABIT_CREATION
            lowerMessage.contains("分析") || lowerMessage.contains("统计") || lowerMessage.contains("进度") -> 
                AssistantContext.PROGRESS_REVIEW
            lowerMessage.contains("错过") || lowerMessage.contains("没完成") -> 
                AssistantContext.MISSED_HABIT
            lowerMessage.contains("完成") || lowerMessage.contains("打卡") -> 
                AssistantContext.HABIT_COMPLETION
            else -> 
                currentContext // Keep current context if no clear indicator
        }
    }

    /**
     * Load suggested actions based on the current context
     */
    private fun loadSuggestedActions() {
        viewModelScope.launch {
            try {
                val suggestions = aiAssistantService.getContextualSuggestions(
                    userId = userId,
                    context = currentContext
                )
                _suggestedActions.value = suggestions
            } catch (e: Exception) {
                // If we can't load suggestions, use default suggestions
                _suggestedActions.value = getDefaultSuggestions()
            }
        }
    }

    /**
     * Get default suggestions if API fails
     */
    private fun getDefaultSuggestions(): List<AssistantAction> {
        return when (currentContext) {
            AssistantContext.HABIT_CREATION -> listOf(
                AssistantAction(
                    type = ActionType.CREATE_HABIT,
                    title = "推荐健康习惯",
                    payload = mapOf("category" to "HEALTH")
                ),
                AssistantAction(
                    type = ActionType.CREATE_HABIT,
                    title = "推荐学习习惯",
                    payload = mapOf("category" to "LEARNING")
                ),
                AssistantAction(
                    type = ActionType.CREATE_HABIT,
                    title = "推荐冥想习惯",
                    payload = mapOf("category" to "MINDFULNESS")
                )
            )
            AssistantContext.PROGRESS_REVIEW -> listOf(
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看完成率趋势",
                    payload = mapOf("type" to "COMPLETION_TREND")
                ),
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看习惯一致性",
                    payload = mapOf("type" to "CONSISTENCY")
                )
            )
            AssistantContext.MISSED_HABIT -> listOf(
                AssistantAction(
                    type = ActionType.MODIFY_HABIT,
                    title = "调整习惯难度",
                    payload = mapOf("adjustment" to "DECREASE_DIFFICULTY")
                ),
                AssistantAction(
                    type = ActionType.MODIFY_HABIT,
                    title = "获取重新开始的建议",
                    payload = mapOf("type" to "RESTART_ADVICE")
                )
            )
            AssistantContext.HABIT_COMPLETION -> listOf(
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看当前连续记录",
                    payload = mapOf("type" to "CURRENT_STREAK")
                ),
                AssistantAction(
                    type = ActionType.CREATE_HABIT,
                    title = "推荐相关习惯",
                    payload = mapOf("type" to "RELATED_HABITS")
                )
            )
            else -> listOf(
                AssistantAction(
                    type = ActionType.CREATE_HABIT,
                    title = "推荐新习惯",
                    payload = null
                ),
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看习惯分析",
                    payload = null
                ),
                AssistantAction(
                    type = ActionType.MODIFY_HABIT,
                    title = "如何坚持习惯？",
                    payload = null
                )
            )
        }
    }
    
    /**
     * Get default actions for welcome message
     */
    private fun getDefaultActions(): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "推荐适合我的习惯",
                payload = null
            ),
            AssistantAction(
                type = ActionType.VIEW_ANALYSIS,
                title = "分析我的习惯数据",
                payload = null
            ),
            AssistantAction(
                type = ActionType.MODIFY_HABIT,
                title = "如何提高习惯坚持度",
                payload = null
            )
        )
    }

    /**
     * Add a message to the chat
     */
    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
    
    /**
     * Clear all messages in the chat
     */
    fun clearChat() {
        _messages.value = emptyList()
        conversationId = null
        currentContext = AssistantContext.GENERAL
        loadSuggestedActions()
    }
}

/**
 * State for the chat UI
 */
sealed class ChatState {
    object Loading : ChatState()
    object Success : ChatState()
    data class Error(val message: String) : ChatState()
}

/**
 * Data class for chat messages
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timestamp: LocalDateTime,
    val actions: List<AssistantAction>
)

/**
 * Enum for message sender
 */
enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}