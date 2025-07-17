package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.AIAssistantRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.data.repository.UserPreferencesRepository
import com.andychen.habitgem.domain.model.ActionType
import com.andychen.habitgem.domain.model.AssistantAction
import com.andychen.habitgem.domain.model.AssistantResponse
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ResponseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AIAssistantServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AIAssistantService {
    
    private val personalizedResponseGenerator = PersonalizedResponseGenerator(
        habitRepository, userPreferencesRepository
    )
    
    private val conversationContexts = ConcurrentHashMap<String, ConversationContext>()
    
    private val habitCategoryPatterns = mapOf(
        "健康" to HabitCategory.HEALTH,
        "锻炼" to HabitCategory.FITNESS,
        "健身" to HabitCategory.FITNESS,
        "冥想" to HabitCategory.MINDFULNESS,
        "正念" to HabitCategory.MINDFULNESS,
        "生产力" to HabitCategory.PRODUCTIVITY,
        "效率" to HabitCategory.PRODUCTIVITY,
        "学习" to HabitCategory.LEARNING,
        "阅读" to HabitCategory.LEARNING,
        "社交" to HabitCategory.SOCIAL,
        "人际关系" to HabitCategory.SOCIAL,
        "创意" to HabitCategory.CREATIVITY,
        "创造力" to HabitCategory.CREATIVITY,
        "财务" to HabitCategory.FINANCE,
        "理财" to HabitCategory.FINANCE
    )
    
    private val intentPatterns = mapOf(
        IntentType.CREATE_HABIT to listOf("创建", "开始", "养成", "建立", "添加", "新习惯", "推荐习惯"),
        IntentType.VIEW_ANALYSIS to listOf("分析", "统计", "数据", "进度", "查看", "了解", "如何做的"),
        IntentType.MODIFY_HABIT to listOf("修改", "调整", "改变", "更新", "编辑"),
        IntentType.GET_MOTIVATION to listOf("动力", "坚持", "激励", "困难", "放弃", "继续", "如何保持"),
        IntentType.LEARN_SCIENCE to listOf("科学", "原理", "研究", "为什么", "如何工作", "证据"),
        IntentType.GENERAL_HELP to listOf("帮助", "怎么用", "功能", "能做什么", "使用方法")
    )
    
    override suspend fun sendMessage(
        userId: String,
        message: String,
        conversationId: String?
    ): AssistantResponse = withContext(Dispatchers.IO) {
        try {
            val context = getOrCreateConversationContext(userId, conversationId)
            context.addMessage(message)
            
            val intent = recognizeIntent(message)
            val entities = extractEntities(message)
            
            context.currentIntent = intent
            context.addEntities(entities)
            
            val response = aiServiceApi.getAssistantResponse(
                AIAssistantRequest(
                    userId = userId,
                    message = message,
                    context = context.toContextString(),
                    conversationId = context.conversationId
                )
            )
            
            if (context.conversationId == null) {
                context.conversationId = response.conversationId
                conversationContexts[getContextKey(userId, response.conversationId)] = context
            }
            
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
            val intent = recognizeIntent(message)
            val entities = extractEntities(message)
            
            return@withContext generateLocalResponse(message, intent, entities)
        }
    }
    
    override suspend fun getContextualSuggestions(
        userId: String,
        context: AssistantContext
    ): List<AssistantAction> = withContext(Dispatchers.IO) {
        try {
            return@withContext when (context) {
                AssistantContext.HABIT_CREATION -> getHabitCreationSuggestions(userId)
                AssistantContext.HABIT_COMPLETION -> getHabitCompletionSuggestions(userId)
                AssistantContext.MISSED_HABIT -> getMissedHabitSuggestions(userId)
                AssistantContext.PROGRESS_REVIEW -> getProgressReviewSuggestions(userId)
                AssistantContext.GENERAL -> getGeneralSuggestions(userId)
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    private fun recognizeIntent(message: String): IntentType {
        val lowerMessage = message.lowercase()
        
        for ((intent, patterns) in intentPatterns) {
            if (patterns.any { pattern -> lowerMessage.contains(pattern) }) {
                return intent
            }
        }
        
        return IntentType.GENERAL_HELP
    }
    
    private fun extractEntities(message: String): Map<String, String> {
        val lowerMessage = message.lowercase()
        val entities = mutableMapOf<String, String>()
        
        for ((keyword, category) in habitCategoryPatterns) {
            if (lowerMessage.contains(keyword)) {
                entities["category"] = category.name
                break
            }
        }
        
        if (lowerMessage.contains("早上") || lowerMessage.contains("早晨") || lowerMessage.contains("起床后")) {
            entities["time_of_day"] = "MORNING"
        } else if (lowerMessage.contains("晚上") || lowerMessage.contains("睡前")) {
            entities["time_of_day"] = "EVENING"
        } else if (lowerMessage.contains("下午")) {
            entities["time_of_day"] = "AFTERNOON"
        }
        
        if (lowerMessage.contains("每天")) {
            entities["frequency"] = "DAILY"
        } else if (lowerMessage.contains("每周") || lowerMessage.contains("一周")) {
            entities["frequency"] = "WEEKLY"
        } else if (lowerMessage.contains("工作日")) {
            entities["frequency"] = "WEEKDAYS"
        } else if (lowerMessage.contains("周末")) {
            entities["frequency"] = "WEEKENDS"
        }
        
        if (lowerMessage.contains("简单") || lowerMessage.contains("容易")) {
            entities["difficulty"] = "EASY"
        } else if (lowerMessage.contains("困难") || lowerMessage.contains("难")) {
            entities["difficulty"] = "HARD"
        }
        
        return entities
    }
    
    private fun getOrCreateConversationContext(userId: String, conversationId: String?): ConversationContext {
        val key = getContextKey(userId, conversationId)
        return conversationContexts[key] ?: ConversationContext(userId, conversationId).also {
            if (conversationId != null) {
                conversationContexts[key] = it
            }
        }
    }
    
    private fun getContextKey(userId: String, conversationId: String?): String {
        return if (conversationId != null) "$userId:$conversationId" else userId
    }
    
    private suspend fun generateLocalResponse(
        message: String,
        intent: IntentType,
        entities: Map<String, String>
    ): AssistantResponse {
        // Generate base response
        val baseResponse = when (intent) {
            IntentType.CREATE_HABIT -> {
                val category = entities["category"]
                val timeOfDay = entities["time_of_day"]
                
                val responseText = if (category != null) {
                    "我可以帮您创建${getCategoryDisplayName(category)}类的习惯。" +
                    (if (timeOfDay != null) "您希望在${getTimeOfDayDisplayName(timeOfDay)}执行这个习惯吗？" else "您想在一天中的什么时间执行这个习惯？")
                } else {
                    "我可以根据您的目标和偏好推荐适合的习惯。您对哪个领域的习惯感兴趣？健康、学习、生产力还是其他？"
                }
                
                val actions = mutableListOf<AssistantAction>()
                
                if (category != null) {
                    when (category) {
                        "HEALTH" -> {
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天喝足够的水",
                                payload = mapOf("category" to "HEALTH", "name" to "每天喝足够的水")
                            ))
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天站立工作30分钟",
                                payload = mapOf("category" to "HEALTH", "name" to "每天站立工作30分钟")
                            ))
                        }
                        "FITNESS" -> {
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天快走10分钟",
                                payload = mapOf("category" to "FITNESS", "name" to "每天快走10分钟")
                            ))
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每周3次力量训练",
                                payload = mapOf("category" to "FITNESS", "name" to "每周3次力量训练")
                            ))
                        }
                        "MINDFULNESS" -> {
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天冥想5分钟",
                                payload = mapOf("category" to "MINDFULNESS", "name" to "每天冥想5分钟")
                            ))
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天深呼吸练习",
                                payload = mapOf("category" to "MINDFULNESS", "name" to "每天深呼吸练习")
                            ))
                        }
                        "LEARNING" -> {
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每天阅读20分钟",
                                payload = mapOf("category" to "LEARNING", "name" to "每天阅读20分钟")
                            ))
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "每周学习新技能1小时",
                                payload = mapOf("category" to "LEARNING", "name" to "每周学习新技能1小时")
                            ))
                        }
                        else -> {
                            actions.add(AssistantAction(
                                type = ActionType.CREATE_HABIT,
                                title = "创建新习惯",
                                payload = mapOf("category" to category)
                            ))
                        }
                    }
                } else {
                    actions.add(AssistantAction(
                        type = ActionType.CREATE_HABIT,
                        title = "健康习惯",
                        payload = mapOf("category" to "HEALTH")
                    ))
                    actions.add(AssistantAction(
                        type = ActionType.CREATE_HABIT,
                        title = "学习习惯",
                        payload = mapOf("category" to "LEARNING")
                    ))
                    actions.add(AssistantAction(
                        type = ActionType.CREATE_HABIT,
                        title = "冥想习惯",
                        payload = mapOf("category" to "MINDFULNESS")
                    ))
                }
                
                AssistantResponse(
                    message = responseText,
                    type = ResponseType.SUGGESTION,
                    relatedHabits = null,
                    actionSuggestions = actions
                )
            }
            
            IntentType.VIEW_ANALYSIS -> {
                AssistantResponse(
                    message = "我可以帮您分析习惯数据，查看完成率、趋势和模式。您想了解哪个习惯的分析？",
                    type = ResponseType.ANALYSIS,
                    relatedHabits = null,
                    actionSuggestions = listOf(
                        AssistantAction(
                            type = ActionType.VIEW_ANALYSIS,
                            title = "查看整体完成率",
                            payload = mapOf("type" to "COMPLETION_RATE")
                        ),
                        AssistantAction(
                            type = ActionType.VIEW_ANALYSIS,
                            title = "查看习惯趋势",
                            payload = mapOf("type" to "TREND")
                        ),
                        AssistantAction(
                            type = ActionType.VIEW_ANALYSIS,
                            title = "查看最佳表现时间",
                            payload = mapOf("type" to "BEST_TIME")
                        )
                    )
                )
            }
            
            IntentType.MODIFY_HABIT -> {
                AssistantResponse(
                    message = "您想要如何调整习惯？我可以帮您修改难度、频率或提醒时间。",
                    type = ResponseType.SUGGESTION,
                    relatedHabits = null,
                    actionSuggestions = listOf(
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
            }
            
            IntentType.GET_MOTIVATION -> {
                AssistantResponse(
                    message = "坚持习惯确实需要一些策略。研究表明，将新习惯与现有习惯关联、设置明确的触发因素，以及庆祝小成就都有助于建立持久的习惯。您想了解更多具体的策略吗？",
                    type = ResponseType.TEXT,
                    relatedHabits = null,
                    actionSuggestions = listOf(
                        AssistantAction(
                            type = ActionType.EXTERNAL_LINK,
                            title = "习惯养成的科学原理",
                            payload = mapOf("url" to "https://habitgem.com/science")
                        ),
                        AssistantAction(
                            type = ActionType.MODIFY_HABIT,
                            title = "调整习惯难度",
                            payload = mapOf("adjustment" to "ADJUST_DIFFICULTY")
                        )
                    )
                )
            }
            
            IntentType.LEARN_SCIENCE -> {
                AssistantResponse(
                    message = "习惯养成的科学原理基于行为心理学研究。根据詹姆斯·克利尔的《原子习惯》，习惯形成包括四个步骤：提示、渴望、反应和奖励。通过理解并优化这些步骤，您可以更有效地建立新习惯。您想了解更多关于哪个方面的信息？",
                    type = ResponseType.TEXT,
                    relatedHabits = null,
                    actionSuggestions = listOf(
                        AssistantAction(
                            type = ActionType.EXTERNAL_LINK,
                            title = "了解习惯循环",
                            payload = mapOf("url" to "https://habitgem.com/science/habit-loop")
                        ),
                        AssistantAction(
                            type = ActionType.EXTERNAL_LINK,
                            title = "习惯养成的时间",
                            payload = mapOf("url" to "https://habitgem.com/science/habit-formation-time")
                        )
                    )
                )
            }
            
            IntentType.GENERAL_HELP -> {
                AssistantResponse(
                    message = "我是您的AI习惯助手，可以帮您推荐习惯、分析进度、提供建议和回答问题。请告诉我您需要什么帮助？",
                    type = ResponseType.TEXT,
                    relatedHabits = null,
                    actionSuggestions = listOf(
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
                )
            }
        }
        
        // Personalize the response using the PersonalizedResponseGenerator
        try {
            val context = getOrCreateConversationContext("user_123", null)
            return personalizedResponseGenerator.generateResponse(
                userId = context.userId,
                intent = intent,
                entities = entities,
                baseResponse = baseResponse
            )
        } catch (e: Exception) {
            // If personalization fails, return the base response
            return baseResponse
        }
    }
    
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
    
    private fun getTimeOfDayDisplayName(timeOfDay: String): String {
        return when (timeOfDay) {
            "MORNING" -> "早晨"
            "AFTERNOON" -> "下午"
            "EVENING" -> "晚上"
            else -> "一天中的某个时间"
        }
    }
    
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
    
    private suspend fun getMissedHabitSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.MODIFY_HABIT,
                title = "调整习惯难度",
                payload = mapOf("adjustment" to "DECREASE_DIFFICULTY")
            ),
            AssistantAction(
                type = ActionType.MODIFY_HABIT,
                title = "更改习惯时间",
                payload = mapOf("adjustment" to "CHANGE_TIME")
            )
        )
    }
    
    private suspend fun getProgressReviewSuggestions(userId: String): List<AssistantAction> {
        return listOf(
            AssistantAction(
                type = ActionType.VIEW_ANALYSIS,
                title = "查看详细分析",
                payload = mapOf("type" to "DETAILED")
            ),
            AssistantAction(
                type = ActionType.VIEW_HABIT,
                title = "查看习惯详情",
                payload = null
            )
        )
    }
    
    private suspend fun getGeneralSuggestions(userId: String): List<AssistantAction> {
        val habits = habitRepository.getHabitsByUserId(userId).first()
        val suggestions = mutableListOf<AssistantAction>()
        
        suggestions.add(
            AssistantAction(
                type = ActionType.CREATE_HABIT,
                title = "创建新习惯",
                payload = null
            )
        )
        
        if (habits.isNotEmpty()) {
            suggestions.add(
                AssistantAction(
                    type = ActionType.VIEW_ANALYSIS,
                    title = "查看习惯分析",
                    payload = null
                )
            )
            
            val mostRecentHabit = habits.maxByOrNull { it.createdAt }
            if (mostRecentHabit != null) {
                suggestions.add(
                    AssistantAction(
                        type = ActionType.VIEW_HABIT,
                        title = "查看「${mostRecentHabit.name}」详情",
                        payload = mapOf("habitId" to mostRecentHabit.id, "habitName" to mostRecentHabit.name)
                    )
                )
            }
        }
        
        return suggestions
    }
}

class ConversationContext(
    val userId: String,
    var conversationId: String? = null
) {
    private val messages = mutableListOf<String>()
    private val entities = mutableMapOf<String, String>()
    var currentIntent: IntentType = IntentType.GENERAL_HELP
    
    fun addMessage(message: String) {
        messages.add(message)
        if (messages.size > 10) {
            messages.removeAt(0)
        }
    }
    
    fun addEntities(newEntities: Map<String, String>) {
        entities.putAll(newEntities)
    }
    
    fun toContextString(): String {
        val contextBuilder = StringBuilder()
        
        contextBuilder.append("intent:${currentIntent.name};")
        
        if (entities.isNotEmpty()) {
            contextBuilder.append("entities:")
            entities.entries.joinTo(contextBuilder, ",") { "${it.key}=${it.value}" }
            contextBuilder.append(";")
        }
        
        return contextBuilder.toString()
    }
}

enum class IntentType {
    CREATE_HABIT,
    VIEW_ANALYSIS,
    MODIFY_HABIT,
    GET_MOTIVATION,
    LEARN_SCIENCE,
    GENERAL_HELP
}