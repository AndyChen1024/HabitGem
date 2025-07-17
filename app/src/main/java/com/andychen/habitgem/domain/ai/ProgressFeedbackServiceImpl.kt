package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.HabitAnalysisRequest
import com.andychen.habitgem.data.api.model.ProgressFeedbackRequest
import com.andychen.habitgem.data.api.model.TimeRangeDto
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.model.AnimationType
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.domain.model.FeedbackMessage
import com.andychen.habitgem.domain.model.FeedbackType
import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecord
import com.andychen.habitgem.domain.model.PeriodicReport
import com.andychen.habitgem.domain.model.ProgressAnalysis
import com.andychen.habitgem.domain.model.ReportPeriod
import com.andychen.habitgem.domain.model.Trend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

/**
 * Implementation of ProgressFeedbackService
 * 
 * This service provides AI-powered feedback for habit completion and progress tracking.
 * It uses a combination of API calls and template-based feedback generation with
 * context-aware variable substitution.
 * 
 * The feedback generator uses a template system with variable substitution to create
 * personalized feedback messages based on user context, habit type, and progress data.
 * It supports multiple feedback types including completion, streak, milestone, and missed habits.
 */
@Singleton
class ProgressFeedbackServiceImpl @Inject constructor(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : ProgressFeedbackService {
    
    /**
     * Template system for different feedback types
     * Each template contains placeholders that will be replaced with context-specific values
     */
    
    /**
     * 反馈模板系统
     * 
     * 这个系统使用分层的模板结构，根据不同的上下文和习惯类型提供个性化的反馈。
     * 每个模板包含可替换的变量，这些变量会在运行时被实际值替换。
     */
    
    // 基础完成模板 - 根据时间、日期和进度提供不同的反馈
    private val completionTemplates = mapOf(
        "standard" to listOf(
            "做得好！您已经完成了今天的{habit_name}。",
            "太棒了！您坚持了{habit_name}，这是成功的一步。",
            "恭喜完成{habit_name}！每一次坚持都在塑造更好的自己。",
            "今天的{habit_name}已完成！保持这个节奏。",
            "您成功完成了{habit_name}！这种积极的行动正在形成强大的习惯。"
        ),
        "morning" to listOf(
            "早安！完成{habit_name}是开始新一天的绝佳方式。",
            "清晨完成{habit_name}，为一整天设定了积极的基调！",
            "早起的鸟儿有虫吃！您今天已经完成了{habit_name}。",
            "早晨的习惯最有力量！您的{habit_name}已经为今天奠定了良好基础。",
            "一日之计在于晨！完成{habit_name}是您今天的第一个胜利。"
        ),
        "afternoon" to listOf(
            "下午好！您已经完成了今天的{habit_name}。",
            "午后时光也不忘{habit_name}，您的坚持令人钦佩！",
            "在一天的中段完成{habit_name}，为下半天注入新能量！",
            "即使在忙碌的下午，您也没有忘记{habit_name}，真是太棒了！",
            "下午完成{habit_name}，保持这种平衡的生活节奏！"
        ),
        "evening" to listOf(
            "在一天结束前完成{habit_name}，真是完美的收尾！",
            "晚上好！您已经成功完成了今天的{habit_name}。",
            "在忙碌的一天后还能坚持{habit_name}，展现了您的决心！",
            "夜晚也是坚持的好时机！您的{habit_name}已经完成。",
            "结束这一天的最佳方式就是完成{habit_name}，做得好！"
        ),
        "night" to listOf(
            "深夜也不忘{habit_name}，您的坚持精神令人敬佩！",
            "即使在夜深人静时，您也坚持完成了{habit_name}。",
            "夜间完成{habit_name}，展示了您对自我提升的承诺！",
            "在这宁静的夜晚，您的{habit_name}已经圆满完成。",
            "夜深了，但您的坚持没有休息，{habit_name}已完成！"
        ),
        "weekend" to listOf(
            "周末也不忘{habit_name}，您的坚持令人钦佩！",
            "即使在休息日，您也没有忘记{habit_name}，真是太棒了！",
            "周末坚持{habit_name}，这种一致性将带来长期的改变。",
            "周末也保持习惯，这正是{habit_name}成功的关键！",
            "休息日也不松懈，您的{habit_name}习惯正在变得更加稳固。"
        ),
        "improvement" to listOf(
            "您的{habit_name}完成率正在提高！继续保持这个势头。",
            "进步显著！您的{habit_name}习惯正在变得更加稳定。",
            "数据显示您的{habit_name}完成情况正在改善，继续加油！",
            "您在{habit_name}上的坚持正在产生积极变化，完成率提高了！",
            "坚持就是胜利！您的{habit_name}完成率比上周提高了。"
        ),
        "category_specific" to mapOf(
            "HEALTH" to listOf(
                "健康的选择！完成{habit_name}正在为您的身体健康加分。",
                "每一次{habit_name}都是对健康的投资，今天您又前进了一步！",
                "坚持健康习惯不容易，但您做到了！{habit_name}已完成。"
            ),
            "FITNESS" to listOf(
                "又一次锻炼完成！您的{habit_name}正在塑造更强健的自己。",
                "坚持{habit_name}，您的身体正在感谢您！",
                "运动的力量不可低估，您的{habit_name}正在改变您的体能水平！"
            ),
            "MINDFULNESS" to listOf(
                "心灵的平静来自于坚持，您的{habit_name}已经完成。",
                "每一次{habit_name}都是对内心的关怀，感受这份宁静。",
                "正念的力量在于坚持，今天的{habit_name}已经为您的心灵充电。"
            ),
            "PRODUCTIVITY" to listOf(
                "效率提升！您的{habit_name}正在帮助您更好地管理时间。",
                "每完成一次{habit_name}，您的生产力就提升一分！",
                "坚持高效习惯的人总能取得更多成就，您的{habit_name}已完成！"
            ),
            "LEARNING" to listOf(
                "知识的积累在于坚持，您的{habit_name}又增添了新的智慧。",
                "学习是终身的旅程，您的{habit_name}正在铺就这条道路。",
                "每一次{habit_name}都是对大脑的锻炼，今天的学习已完成！"
            ),
            "SOCIAL" to listOf(
                "人际关系需要经营，您的{habit_name}正在强化这些联系。",
                "社交能力也是一种技能，通过{habit_name}您正在提升它！",
                "每一次{habit_name}都在丰富您的社交网络，做得好！"
            ),
            "CREATIVITY" to listOf(
                "创意的火花需要持续的燃料，您的{habit_name}正在提供它！",
                "艺术来自于坚持，您的{habit_name}正在释放创造力。",
                "每一次{habit_name}都是对创造力的锻炼，今天的创作已完成！"
            ),
            "FINANCE" to listOf(
                "财务健康需要日常维护，您的{habit_name}正在构建稳固的基础。",
                "每一次{habit_name}都是对财务未来的投资，明智的选择！",
                "金钱管理是一种习惯，您的{habit_name}正在培养这种能力。"
            ),
            "OTHER" to listOf(
                "坚持的力量无可估量，您的{habit_name}正在改变生活！",
                "每一个小习惯都能带来大改变，您的{habit_name}已完成。",
                "生活质量来自于日常选择，您的{habit_name}是明智之举。"
            )
        )
    )
    
    // Streak-based templates with more context variables
    private val streakTemplates = mapOf(
        "standard" to listOf(
            "令人印象深刻！您已经连续{streak}天坚持{habit_name}了。",
            "连续{streak}天！您的{habit_name}习惯正在稳步形成。",
            "🔥 {streak}天连续打卡！您的{habit_name}习惯正在变得更加牢固。",
            "坚持就是胜利！已经连续{streak}天完成{habit_name}了。",
            "连续{streak}天的坚持！您的毅力令人钦佩。"
        ),
        "science" to listOf(
            "科学表明，21天可以形成习惯，您已经连续{streak}天完成{habit_name}了！",
            "研究显示，持续的习惯会重塑大脑神经通路。您的{streak}天{habit_name}正在产生真正的变化！",
            "您已经连续{streak}天完成{habit_name}！根据习惯形成研究，您正处于关键期。"
        ),
        "motivation" to listOf(
            "{streak}天的坚持不是偶然，而是您决心的体现。继续您的{habit_name}！",
            "每一天的{habit_name}都在累积，{streak}天的坚持正在改变您的生活轨迹！",
            "连续{streak}天！您对{habit_name}的坚持展示了真正的自律精神。"
        ),
        "almost_milestone" to listOf(
            "再坚持{days_to_milestone}天就能达到{next_milestone}天里程碑！继续您的{habit_name}！",
            "您已经连续{streak}天完成{habit_name}，距离{next_milestone}天里程碑只有{days_to_milestone}天了！",
            "坚持就是胜利！再{days_to_milestone}天您就将达成{next_milestone}天的{habit_name}里程碑！"
        )
    )
    
    // Milestone templates with achievement emphasis
    private val milestoneTemplates = mapOf(
        "standard" to listOf(
            "🏆 重大里程碑！您已经坚持{habit_name}整整{streak}天了！",
            "恭喜您达成{streak}天的里程碑！您的{habit_name}习惯已经形成。",
            "这是值得庆祝的时刻！{streak}天的{habit_name}，您做到了！",
            "坚持{streak}天是一个了不起的成就！您的{habit_name}习惯已经成为生活的一部分。",
            "您已经坚持{habit_name}{streak}天了！这种持续的努力正在改变您的生活。"
        ),
        "science_backed" to listOf(
            "🎉 {streak}天里程碑！科学研究表明，这个阶段{habit_name}已经开始成为您的自然行为。",
            "恭喜达成{streak}天！根据习惯形成理论，您的{habit_name}已经开始自动化。",
            "了不起的{streak}天！研究显示，这个阶段的坚持会使{habit_name}变成长期记忆。"
        ),
        "transformation" to listOf(
            "{streak}天的{habit_name}不仅是数字，更是生活方式的转变！",
            "整整{streak}天的坚持！您的{habit_name}已经从刻意练习变成了自然行为。",
            "恭喜达成{streak}天里程碑！您的{habit_name}已经成为您身份的一部分。"
        ),
        "category_specific" to listOf(
            "{streak}天的{habit_name}！在{category}领域，您已经取得了显著的进步。",
            "在{category}方面坚持{streak}天，您的{habit_name}正在带来真正的改变！",
            "恭喜您在{category}领域坚持{habit_name}{streak}天！这是专注和毅力的证明。"
        )
    )
    
    // Missed habit templates with recovery focus
    private val missedTemplates = mapOf(
        "standard" to listOf(
            "没关系，每个人都有起伏。明天继续您的{habit_name}吧！",
            "今天错过了{habit_name}？别担心，重新开始永远不晚。",
            "坚持习惯是一场马拉松，而不是短跑。明天再继续{habit_name}吧。",
            "暂时的中断不会影响长期进步。明天继续{habit_name}的旅程吧！",
            "每个成功的人都经历过挫折。明天是{habit_name}的新机会！"
        ),
        "science" to listOf(
            "研究表明，习惯形成过程中的偶尔中断不会影响长期成功。明天继续您的{habit_name}！",
            "科学告诉我们，完美不是目标，一致性才是。继续您的{habit_name}之旅！",
            "根据习惯研究，重要的是快速恢复而不是完美记录。明天再战{habit_name}！"
        ),
        "high_previous_completion" to listOf(
            "您之前的{habit_name}完成率达到了{completion_rate}%，一次中断不会改变这一成就！",
            "考虑到您之前{completion_rate}%的完成率，这只是{habit_name}旅程中的小波折。",
            "您的{habit_name}整体完成率为{completion_rate}%，这次中断只是暂时的！"
        ),
        "streak_recovery" to listOf(
            "您之前已经连续坚持了{previous_streak}天！明天开始新的{habit_name}连续记录吧。",
            "之前的{previous_streak}天连续记录证明您能做到！重新开始您的{habit_name}。",
            "您曾经连续{previous_streak}天完成{habit_name}，您有能力再次做到！"
        )
    )
    
    // Enhanced emoji mappings for different habit categories with more variety
    private val categoryEmojis = mapOf(
        "HEALTH" to listOf("🍎", "💊", "🥦", "💪", "❤️", "🥗", "🧠", "😴", "🌿", "🍵"),
        "FITNESS" to listOf("🏃", "🏋️", "🧘", "🚴", "🏊", "⚽", "🏆", "🤸", "🧗", "🥇"),
        "MINDFULNESS" to listOf("🧠", "🧘", "✨", "🌈", "🌱", "🌞", "🌙", "🌊", "🕯️", "☮️"),
        "PRODUCTIVITY" to listOf("📊", "✅", "⏱️", "📝", "💼", "🎯", "📈", "💡", "🔍", "📌"),
        "LEARNING" to listOf("📚", "🎓", "💡", "🔍", "🧩", "🔬", "🌐", "📖", "✏️", "🧮"),
        "SOCIAL" to listOf("👥", "🗣️", "🤝", "💬", "🎭", "👋", "🫂", "🎉", "👨‍👩‍👧‍👦", "🤗"),
        "CREATIVITY" to listOf("🎨", "🎵", "✏️", "🎬", "📷", "🎭", "🎤", "🖌️", "🧶", "🎹"),
        "FINANCE" to listOf("💰", "💹", "💵", "📊", "🏦", "💳", "💼", "📈", "🪙", "💸"),
        "OTHER" to listOf("🌟", "🎯", "🔄", "🎁", "🌈", "🔔", "🌻", "🍀", "🌠", "🎪")
    )
    
    // Enhanced animation type mappings with more context
    private val animationMappings = mapOf(
        FeedbackType.COMPLETION to AnimationType.THUMBS_UP,
        FeedbackType.STREAK to AnimationType.SPARKLE,
        FeedbackType.MILESTONE to AnimationType.FIREWORKS,
        FeedbackType.MISSED to AnimationType.NONE
    )
    
    // Milestone thresholds for feedback
    private val milestoneThresholds = listOf(7, 21, 30, 60, 90, 180, 365)
    
    // Time of day definitions
    private val morningTimeRange = LocalTime.of(5, 0)..LocalTime.of(11, 59)
    private val afternoonTimeRange = LocalTime.of(12, 0)..LocalTime.of(17, 59)
    private val eveningTimeRange = LocalTime.of(18, 0)..LocalTime.of(23, 59)
    private val nightTimeRange = LocalTime.of(0, 0)..LocalTime.of(4, 59)
    
    /**
     * Get personalized feedback when a habit is completed
     * 
     * This method generates personalized feedback based on:
     * 1. User's habit completion history
     * 2. Current streak and milestone status
     * 3. Time of day and day of week patterns
     * 4. Habit category and difficulty
     * 5. Progress trends and correlations with other habits
     * 
     * It first attempts to get feedback from the AI service API, and falls back
     * to template-based feedback generation if the API call fails.
     * 
     * @param userId User ID
     * @param habitId Habit ID
     * @return Personalized feedback message
     */
    override suspend fun getCompletionFeedback(userId: String, habitId: String): FeedbackMessage = withContext(Dispatchers.IO) {
        try {
            // Get habit details
            val habit = habitRepository.getHabitById(habitId).first() 
                ?: throw IllegalArgumentException("Habit not found")
            
            // Get current streak and completion data
            val streak = habitRepository.getCurrentStreak(habitId).first()
            val completionRate = habitRepository.getCompletionRate(habitId).first()
            
            // Get recent records to analyze patterns
            val today = LocalDate.now()
            val lastWeekRecords = habitRepository.getHabitRecordsByDateRange(
                userId = userId,
                startDate = today.minusDays(7),
                endDate = today
            ).first().filter { it.habitId == habitId }
            
            // Calculate time of day pattern
            val timeOfDayPattern = calculateTimeOfDayPattern(lastWeekRecords)
            
            // Calculate best performing days
            val bestDays = calculateBestPerformingDays(lastWeekRecords)
            
            // Perform advanced pattern analysis
            val patternData = analyzeHabitPatterns(userId, habitId, lastWeekRecords)
            
            // Determine feedback type based on context
            val feedbackType = when {
                streak >= 30 -> FeedbackType.MILESTONE
                streak >= 7 -> FeedbackType.STREAK
                else -> FeedbackType.COMPLETION
            }
            
            // Create rich context data
            val contextData = buildContextData(habit, streak, completionRate, timeOfDayPattern, bestDays)
            
            // Add pattern analysis data to context
            contextData.putAll(patternData)
            
            // Add current time context
            val currentTime = LocalTime.now()
            contextData["time_of_day"] = when {
                currentTime in morningTimeRange -> "morning"
                currentTime in afternoonTimeRange -> "afternoon"
                currentTime in eveningTimeRange -> "evening"
                else -> "night"
            }
            
            // Add day of week context
            val currentDayOfWeek = LocalDate.now().dayOfWeek
            contextData["is_weekend"] = (currentDayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)).toString()
            contextData["day_of_week"] = currentDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            
            // Try to get feedback from API
            try {
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
                // Fallback to template-based feedback if API fails
                return@withContext generateTemplateFeedback(
                    habit = habit,
                    streak = streak,
                    feedbackType = feedbackType,
                    contextData = contextData
                )
            }
        } catch (e: Exception) {
            // Ultimate fallback for any errors
            return@withContext FeedbackMessage(
                message = "做得好！继续保持！",
                type = FeedbackType.COMPLETION,
                emoji = "👍",
                animationType = AnimationType.THUMBS_UP
            )
        }
    }
    
    /**
     * 获取习惯进度分析
     * 
     * 此方法分析用户的习惯数据，生成个性化的进度分析，包括：
     * 1. 完成率和连续天数统计
     * 2. 基于历史数据的模式识别
     * 3. 个性化的洞察和建议
     * 4. 可视化数据点
     * 
     * @param userId 用户ID
     * @param habitId 习惯ID
     * @return 进度分析结果
     */
    override suspend fun getProgressAnalysis(userId: String, habitId: String): ProgressAnalysis = withContext(Dispatchers.IO) {
        try {
            // 获取习惯详情
            val habit = habitRepository.getHabitById(habitId).first()
                ?: throw IllegalArgumentException("Habit not found")
            
            // 获取基本统计数据
            val completionRate = habitRepository.getCompletionRate(habitId).first()
            val streak = habitRepository.getCurrentStreak(habitId).first()
            val longestStreak = habitRepository.getLongestStreak(habitId).first()
            
            // 获取历史记录数据
            val today = LocalDate.now()
            val startDate = today.minusDays(30) // 分析最近30天的数据
            val records = habitRepository.getHabitRecordsByDateRange(
                habitId = habitId,
                startDate = startDate,
                endDate = today
            ).first()
            
            // 生成最近7天的数据点
            val weekDataPoints = (0..6).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val record = records.find { it.date == date }
                DataPoint(
                    date = date,
                    value = if (record?.isCompleted == true) 1f else 0f,
                    label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                )
            }.reversed()
            
            // 分析最佳表现日
            val bestDays = calculateBestPerformingDays(records)
            val bestDayNames = bestDays.map { 
                it.getDisplayName(TextStyle.FULL, Locale.getDefault()) 
            }.joinToString("、")
            
            // 分析最佳时间段
            val bestTimeOfDay = identifyBestTimeOfDay(records)
            val bestTimeText = when (bestTimeOfDay) {
                "morning" -> "早晨"
                "afternoon" -> "下午"
                "evening" -> "晚上"
                "night" -> "夜间"
                else -> null
            }
            
            // 分析完成趋势
            val trend = calculateCompletionTrend(records)
            val trendText = when (trend) {
                Trend.IMPROVING -> "您的完成率正在稳步提高，继续保持！"
                Trend.STABLE -> "您的完成率保持稳定，习惯正在形成。"
                Trend.DECLINING -> "您的完成率有所下降，可能需要调整策略。"
                Trend.FLUCTUATING -> "您的完成率波动较大，尝试建立更稳定的习惯模式。"
                Trend.NOT_ENOUGH_DATA -> null
            }
            
            // 分析难度趋势
            val difficultyTrend = calculateDifficultyTrend(records)
            val difficultyText = when (difficultyTrend) {
                "getting_easier" -> "这个习惯对您来说正在变得越来越容易，这是好现象！"
                "getting_harder" -> "这个习惯似乎正在变得更具挑战性，考虑调整难度或寻求支持。"
                else -> null
            }
            
            // 生成洞察
            val insights = mutableListOf<String>()
            
            // 添加完成率洞察
            when {
                completionRate >= 0.8f -> insights.add("您的完成率非常高 (${(completionRate * 100).toInt()}%)，表明这个习惯已经很好地融入了您的日常生活。")
                completionRate >= 0.5f -> insights.add("您的完成率良好 (${(completionRate * 100).toInt()}%)，继续保持这个势头。")
                completionRate > 0f -> insights.add("您的完成率为 ${(completionRate * 100).toInt()}%，还有提升空间。")
            }
            
            // 添加连续天数洞察
            when {
                streak >= 21 -> insights.add("恭喜您已经连续完成 $streak 天！科学研究表明，21天是形成习惯的关键期。")
                streak >= 7 -> insights.add("您已经连续完成 $streak 天，正在建立稳定的习惯模式。")
                streak > 0 -> insights.add("您当前的连续完成天数是 $streak 天，继续坚持！")
            }
            
            // 添加最佳表现日洞察
            if (bestDays.isNotEmpty()) {
                insights.add("您在$bestDayNames的完成率最高，这些天可能是您最适合执行这个习惯的时间。")
            }
            
            // 添加最佳时间段洞察
            if (bestTimeText != null) {
                insights.add("数据显示您在$bestTimeText时段完成习惯的频率最高，这可能是您的最佳习惯时间。")
            }
            
            // 添加趋势洞察
            if (trendText != null) {
                insights.add(trendText)
            }
            
            // 添加难度洞察
            if (difficultyText != null) {
                insights.add(difficultyText)
            }
            
            // 添加最长连续记录洞察
            if (longestStreak > streak && longestStreak >= 7) {
                insights.add("您的历史最长连续记录是 $longestStreak 天。您有能力再次达到并超越这个记录！")
            }
            
            // 生成建议
            val suggestions = mutableListOf<String>()
            
            // 基于完成率的建议
            when {
                completionRate < 0.3f -> {
                    suggestions.add("考虑将习惯分解为更小、更容易实现的步骤，逐步建立。")
                    suggestions.add("设置每日提醒，帮助您记住完成习惯。")
                }
                completionRate < 0.7f -> {
                    suggestions.add("尝试将这个习惯与您已有的日常活动关联起来，形成触发机制。")
                }
            }
            
            // 基于最佳时间的建议
            if (bestTimeText != null) {
                suggestions.add("尝试在$bestTimeText时段完成这个习惯，这似乎是您最容易坚持的时间。")
            }
            
            // 基于习惯类别的建议
            when (habit.category) {
                HabitCategory.FITNESS -> suggestions.add("研究表明，与朋友一起锻炼可以提高坚持度，考虑邀请朋友一起参与。")
                HabitCategory.LEARNING -> suggestions.add("尝试使用间隔重复技术来提高学习效果，每天花少量时间复习之前学过的内容。")
                HabitCategory.MINDFULNESS -> suggestions.add("即使只有5分钟的正念练习也能带来益处，在忙碌的日子里不要完全跳过，而是缩短时间。")
                HabitCategory.PRODUCTIVITY -> suggestions.add("考虑使用番茄工作法（25分钟专注工作，5分钟休息）来提高效率。")
                HabitCategory.HEALTH -> suggestions.add("健康习惯最好在固定时间执行，尝试建立一个一致的时间表。")
                else -> {}
            }
            
            // 基于趋势的建议
            when (trend) {
                Trend.DECLINING -> suggestions.add("您的完成率有所下降，考虑重新评估您的目标或调整习惯的难度。")
                Trend.FLUCTUATING -> suggestions.add("尝试使用习惯追踪应用或日记来记录影响您习惯执行的因素。")
                else -> {}
            }
            
            // 确保至少有一条洞察和建议
            if (insights.isEmpty()) {
                insights.add("继续记录您的习惯，随着数据积累我们将提供更个性化的分析。")
            }
            
            if (suggestions.isEmpty()) {
                suggestions.add("保持一致性是习惯养成的关键，尽量在固定的时间完成这个习惯。")
            }
            
            // 选择最相关的洞察和建议
            val primaryInsight = insights.firstOrNull() ?: "继续坚持记录，数据将帮助我们提供更准确的分析。"
            val primarySuggestion = suggestions.firstOrNull() ?: "尝试设置提醒，帮助您更好地坚持习惯。"
            
            // 尝试通过API获取更个性化的分析
            try {
                val analysisRequest = HabitAnalysisRequest(
                    userId = userId,
                    habitId = habitId,
                    analysisType = "progress",
                    timeRange = TimeRangeDto(
                        startDate = startDate.toString(),
                        endDate = today.toString()
                    )
                )
                
                val response = aiServiceApi.getHabitAnalysis(analysisRequest)
                
                // 如果API返回了有效的洞察和建议，使用它们
                if (response.insights.isNotEmpty()) {
                    val apiInsight = response.insights.first().insightMessage
                    if (apiInsight.isNotEmpty()) {
                        return@withContext ProgressAnalysis(
                            completionRate = completionRate,
                            streak = streak,
                            insight = apiInsight,
                            suggestion = response.suggestions?.firstOrNull()?.message ?: primarySuggestion,
                            visualData = weekDataPoints
                        )
                    }
                }
            } catch (e: Exception) {
                // API调用失败，使用本地生成的分析
                e.printStackTrace()
            }
            
            // 返回本地生成的分析
            return@withContext ProgressAnalysis(
                completionRate = completionRate,
                streak = streak,
                insight = primaryInsight,
                suggestion = primarySuggestion,
                visualData = weekDataPoints
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // 返回通用分析（出错时）
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
    
    /**
     * Generate template-based feedback based on context
     * 
     * @param habit The habit for which feedback is being generated
     * @param streak Current streak for the habit
     * @param feedbackType Type of feedback to generate
     * @param contextData Additional context data for personalization
     * @return Personalized feedback message
     */
    private fun generateTemplateFeedback(
        habit: Habit,
        streak: Int,
        feedbackType: FeedbackType,
        contextData: Map<String, String>
    ): FeedbackMessage {
        // Select template category based on context
        val templateCategory = selectTemplateCategory(habit, streak, feedbackType, contextData)
        
        // Select template from the appropriate category
        val template = when (feedbackType) {
            FeedbackType.COMPLETION -> selectTemplate(completionTemplates, templateCategory)
            FeedbackType.STREAK -> selectTemplate(streakTemplates, templateCategory)
            FeedbackType.MILESTONE -> selectTemplate(milestoneTemplates, templateCategory)
            FeedbackType.MISSED -> selectTemplate(missedTemplates, templateCategory)
            else -> selectTemplate(completionTemplates, "standard")
        }
        
        // Fill in template variables
        val message = fillTemplateVariables(template, habit, streak, contextData)
        
        // Select appropriate emoji based on habit category
        val emoji = selectEmoji(habit.category)
        
        // Select animation type based on feedback type
        val animationType = animationMappings[feedbackType] ?: AnimationType.NONE
        
        return FeedbackMessage(
            message = message,
            type = feedbackType,
            emoji = emoji,
            animationType = animationType
        )
    }
    
    /**
     * Select appropriate template category based on context
     */
    private fun selectTemplateCategory(
        habit: Habit,
        streak: Int,
        feedbackType: FeedbackType,
        contextData: Map<String, String>
    ): String {
        return when (feedbackType) {
            FeedbackType.COMPLETION -> {
                when {
                    contextData["time_of_day"] == "morning" -> "morning"
                    contextData["time_of_day"] == "evening" -> "evening"
                    contextData["is_weekend"] == "true" -> "weekend"
                    contextData["improving_trend"] == "true" -> "improvement"
                    else -> "standard"
                }
            }
            FeedbackType.STREAK -> {
                when {
                    isApproachingMilestone(streak) -> "almost_milestone"
                    streak >= 14 -> "science"
                    else -> "motivation"
                }
            }
            FeedbackType.MILESTONE -> {
                when {
                    streak >= 60 -> "transformation"
                    streak >= 21 -> "science_backed"
                    else -> "category_specific"
                }
            }
            FeedbackType.MISSED -> {
                val completionRate = contextData["completion_rate"]?.toFloatOrNull() ?: 0f
                val previousStreak = contextData["previous_streak"]?.toIntOrNull() ?: 0
                
                when {
                    completionRate > 0.7 -> "high_previous_completion"
                    previousStreak > 5 -> "streak_recovery"
                    else -> "science"
                }
            }
            else -> "standard"
        }
    }
    
    /**
     * Select a template from the specified category
     * Handles both simple list templates and nested category-specific templates
     */
    private fun selectTemplate(templates: Map<String, Any>, category: String): String {
        // Handle category_specific templates differently
        if (category == "category_specific") {
            val categorySpecificTemplates = templates["category_specific"] as? Map<*, *>
            if (categorySpecificTemplates != null) {
                // Get templates for the specific habit category
                val habitCategoryTemplates = categorySpecificTemplates[contextHabitCategory] as? List<String>
                if (habitCategoryTemplates != null && habitCategoryTemplates.isNotEmpty()) {
                    return habitCategoryTemplates[Random.nextInt(habitCategoryTemplates.size)]
                }
                // Fallback to OTHER category if specific category not found
                val otherTemplates = categorySpecificTemplates["OTHER"] as? List<String>
                if (otherTemplates != null && otherTemplates.isNotEmpty()) {
                    return otherTemplates[Random.nextInt(otherTemplates.size)]
                }
            }
            // If category_specific handling fails, fall back to standard
            return selectTemplate(templates, "standard")
        }
        
        // Handle regular template lists
        val categoryTemplates = templates[category] as? List<String> 
            ?: templates["standard"] as? List<String> 
            ?: listOf("做得好！继续保持！")
        
        return categoryTemplates[Random.nextInt(categoryTemplates.size)]
    }
    
    // Thread-local context for template generation
    private var contextHabitCategory: String = "OTHER"
    
    /**
     * Fill in template variables with actual values
     */
    private fun fillTemplateVariables(
        template: String,
        habit: Habit,
        streak: Int,
        contextData: Map<String, String>
    ): String {
        // Prepare enhanced context data with special variables
        val enhancedContextData = contextData.toMutableMap()
        
        // Add special variables for milestone-related templates
        if (template.contains("{next_milestone}") || template.contains("{days_to_milestone}")) {
            val nextMilestone = findNextMilestone(streak)
            val daysToMilestone = nextMilestone - streak
            
            enhancedContextData["next_milestone"] = nextMilestone.toString()
            enhancedContextData["days_to_milestone"] = daysToMilestone.toString()
        }
        
        // Add special variable for completion rate percentage
        if (template.contains("{completion_rate}")) {
            val completionRate = (contextData["completion_rate"]?.toFloatOrNull() ?: 0f) * 100
            enhancedContextData["completion_rate"] = completionRate.toInt().toString()
        }
        
        // Use the enhanced template processor
        return processTemplateVariables(template, habit, streak, enhancedContextData)
    }
    
    /**
     * Select an appropriate emoji based on habit category
     */
    private fun selectEmoji(category: HabitCategory): String {
        val categoryName = category.name
        val emojis = categoryEmojis[categoryName] ?: categoryEmojis["OTHER"]!!
        return emojis[Random.nextInt(emojis.size)]
    }
    
    /**
     * Check if the current streak is approaching a milestone
     */
    private fun isApproachingMilestone(streak: Int): Boolean {
        val nextMilestone = findNextMilestone(streak)
        val daysToMilestone = nextMilestone - streak
        return daysToMilestone in 1..3 // Within 3 days of a milestone
    }
    
    /**
     * Find the next milestone threshold
     */
    private fun findNextMilestone(streak: Int): Int {
        return milestoneThresholds.find { it > streak } ?: (streak + 30)
    }
    
    /**
     * Calculate time of day pattern from habit records
     */
    private fun calculateTimeOfDayPattern(records: List<HabitRecord>): String {
        val completedRecords = records.filter { it.isCompleted && it.completionTime != null }
        if (completedRecords.isEmpty()) return "unknown"
        
        val timeGroups = completedRecords.groupBy { record ->
            val time = record.completionTime!!.toLocalTime()
            when {
                time in morningTimeRange -> "morning"
                time in afternoonTimeRange -> "afternoon"
                time in eveningTimeRange -> "evening"
                else -> "night"
            }
        }
        
        return timeGroups.maxByOrNull { it.value.size }?.key ?: "unknown"
    }
    
    /**
     * Calculate best performing days from habit records
     */
    private fun calculateBestPerformingDays(records: List<HabitRecord>): List<DayOfWeek> {
        val dayCompletionMap = DayOfWeek.values().associateWith { 0 }.toMutableMap()
        val dayCountMap = DayOfWeek.values().associateWith { 0 }.toMutableMap()
        
        records.forEach { record ->
            val day = record.date.dayOfWeek
            dayCountMap[day] = dayCountMap[day]!! + 1
            if (record.isCompleted) {
                dayCompletionMap[day] = dayCompletionMap[day]!! + 1
            }
        }
        
        // Calculate completion rate for each day
        val dayCompletionRates = DayOfWeek.values().associateWith { day ->
            val count = dayCountMap[day] ?: 0
            val completions = dayCompletionMap[day] ?: 0
            if (count > 0) completions.toFloat() / count else 0f
        }
        
        // Return days with completion rate > 0.5, sorted by completion rate
        return dayCompletionRates.filter { it.value > 0.5 }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    /**
     * Build context data map for feedback generation
     */
    private fun buildContextData(
        habit: Habit,
        streak: Int,
        completionRate: Float,
        timeOfDayPattern: String,
        bestDays: List<DayOfWeek>
    ): Map<String, String> {
        val contextData = mutableMapOf<String, String>()
        
        // Basic habit info
        contextData["habit_name"] = habit.name
        contextData["habit_category"] = habit.category.name
        contextData["habit_difficulty"] = habit.difficulty.toString()
        
        // Performance metrics
        contextData["streak"] = streak.toString()
        contextData["completion_rate"] = completionRate.toString()
        
        // Time patterns
        contextData["time_of_day"] = timeOfDayPattern
        contextData["is_weekend"] = (LocalDate.now().dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)).toString()
        
        // Best days
        if (bestDays.isNotEmpty()) {
            contextData["best_days"] = bestDays.joinToString(",") { 
                it.getDisplayName(TextStyle.FULL, Locale.getDefault()) 
            }
        }
        
        // Milestone info
        val nextMilestone = findNextMilestone(streak)
        contextData["next_milestone"] = nextMilestone.toString()
        contextData["days_to_milestone"] = (nextMilestone - streak).toString()
        
        // Scientific basis if available
        habit.scientificBasis?.let {
            if (it.isNotEmpty()) {
                contextData["scientific_basis"] = "true"
            }
        }
        
        return contextData
    }
    
    /**
     * Extension function to capitalize first letter of a string
     */
    private fun String.capitalize(): String {
        return if (this.isEmpty()) this else this[0].uppercase() + this.substring(1)
    }
}    /
**
     * Template variable processor
     * 
     * This class handles the processing of template variables with more advanced features:
     * - Support for conditional text based on variable values
     * - Support for formatting options
     * - Support for default values
     * 
     * Format: {variable_name:format:default}
     * Examples:
     * - {streak} - Simple variable
     * - {streak:+} - Add "+" sign for positive numbers
     * - {completion_rate:%} - Format as percentage
     * - {habit_name:upper} - Convert to uppercase
     * - {habit_name::未命名} - With default value
     */
    private class TemplateVariableProcessor(
        private val habit: Habit,
        private val streak: Int,
        private val contextData: Map<String, String>
    ) {
        /**
         * Process all variables in a template
         */
        fun process(template: String): String {
            var result = template
            
            // Find all variable patterns in the template
            val pattern = Regex("\\{([^{}]+)\\}")
            val matches = pattern.findAll(template)
            
            for (match in matches) {
                val fullMatch = match.value
                val variableParts = match.groupValues[1].split(":")
                
                val variableName = variableParts[0]
                val format = if (variableParts.size > 1) variableParts[1] else ""
                val defaultValue = if (variableParts.size > 2) variableParts[2] else ""
                
                // Get variable value
                val value = getVariableValue(variableName, defaultValue)
                
                // Apply formatting
                val formattedValue = formatValue(value, format)
                
                // Replace in template
                result = result.replace(fullMatch, formattedValue)
            }
            
            return result
        }
        
        /**
         * Get the value for a variable
         */
        private fun getVariableValue(variableName: String, defaultValue: String): String {
            return when (variableName) {
                "habit_name" -> habit.name
                "streak" -> streak.toString()
                "category" -> habit.category.name.toLowerCase().capitalize()
                "difficulty" -> habit.difficulty.toString()
                else -> contextData[variableName] ?: defaultValue
            }
        }
        
        /**
         * Format a value based on format specifier
         */
        private fun formatValue(value: String, format: String): String {
            return when (format) {
                "upper" -> value.uppercase()
                "lower" -> value.lowercase()
                "+" -> if (value.toIntOrNull()?.let { it > 0 } == true) "+$value" else value
                "%" -> try {
                    val numValue = value.toFloatOrNull() ?: return value
                    "${(numValue * 100).toInt()}%"
                } catch (e: Exception) {
                    value
                }
                else -> value
            }
        }
    }
    
    /**
     * Enhanced template variable substitution
     */
    private fun processTemplateVariables(
        template: String,
        habit: Habit,
        streak: Int,
        contextData: Map<String, String>
    ): String {
        // Store habit category for category-specific template selection
        contextHabitCategory = habit.category.name
        
        // Use the template processor
        return TemplateVariableProcessor(habit, streak, contextData).process(template)
    }    /**

     * Analyze habit patterns to generate more personalized feedback
     * This method analyzes the user's habit data to identify patterns and trends
     * that can be used to generate more personalized feedback
     */
    private suspend fun analyzeHabitPatterns(
        userId: String,
        habitId: String,
        lastWeekRecords: List<HabitRecord>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val patternData = mutableMapOf<String, String>()
        
        try {
            // Get all records for this habit
            val allRecords = habitRepository.getAllHabitRecords(habitId).first()
            
            // Calculate completion trend
            val trend = calculateCompletionTrend(allRecords)
            patternData["trend"] = trend.name
            
            // Identify best time of day
            val bestTimeOfDay = identifyBestTimeOfDay(allRecords)
            if (bestTimeOfDay != null) {
                patternData["best_time_of_day"] = bestTimeOfDay
            }
            
            // Check if user is improving
            val isImproving = isUserImproving(allRecords)
            patternData["improving_trend"] = isImproving.toString()
            
            // Calculate consistency score
            val consistencyScore = calculateConsistencyScore(allRecords)
            patternData["consistency_score"] = consistencyScore.toString()
            
            // Check if habit is becoming easier
            val difficultyTrend = calculateDifficultyTrend(allRecords)
            patternData["difficulty_trend"] = difficultyTrend
            
            // Check for habit correlations
            val correlatedHabits = findCorrelatedHabits(userId, habitId)
            if (correlatedHabits.isNotEmpty()) {
                patternData["correlated_habits"] = correlatedHabits.joinToString(",")
            }
        } catch (e: Exception) {
            // Log error but continue with available data
            e.printStackTrace()
        }
        
        return@withContext patternData
    }
    
    /**
     * Calculate completion trend based on historical data
     */
    private fun calculateCompletionTrend(records: List<HabitRecord>): Trend {
        if (records.size < 7) return Trend.NOT_ENOUGH_DATA
        
        // Group records by week
        val recordsByWeek = records.groupBy { record ->
            record.date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
        }.toSortedMap()
        
        if (recordsByWeek.size < 2) return Trend.NOT_ENOUGH_DATA
        
        // Calculate completion rate for each week
        val weeklyCompletionRates = recordsByWeek.map { (_, weekRecords) ->
            weekRecords.count { it.isCompleted }.toFloat() / weekRecords.size
        }
        
        // Calculate trend
        val recentWeeks = weeklyCompletionRates.takeLast(4)
        if (recentWeeks.size < 2) return Trend.NOT_ENOUGH_DATA
        
        // Check if consistently improving
        val isImproving = recentWeeks.zipWithNext().all { (prev, next) -> next >= prev }
        
        // Check if consistently declining
        val isDeclining = recentWeeks.zipWithNext().all { (prev, next) -> next <= prev }
        
        // Check if stable (within 10% variation)
        val isStable = recentWeeks.zipWithNext().all { (prev, next) -> 
            kotlin.math.abs(next - prev) < 0.1f 
        }
        
        return when {
            isImproving -> Trend.IMPROVING
            isDeclining -> Trend.DECLINING
            isStable -> Trend.STABLE
            else -> Trend.FLUCTUATING
        }
    }
    
    /**
     * Identify the best time of day for habit completion
     */
    private fun identifyBestTimeOfDay(records: List<HabitRecord>): String? {
        val completedRecords = records.filter { it.isCompleted && it.completionTime != null }
        if (completedRecords.isEmpty()) return null
        
        // Group by time of day
        val timeGroups = completedRecords.groupBy { record ->
            val time = record.completionTime!!.toLocalTime()
            when {
                time in morningTimeRange -> "morning"
                time in afternoonTimeRange -> "afternoon"
                time in eveningTimeRange -> "evening"
                else -> "night"
            }
        }
        
        // Find the time of day with highest completion count
        return timeGroups.maxByOrNull { it.value.size }?.key
    }
    
    /**
     * Check if user's habit performance is improving
     */
    private fun isUserImproving(records: List<HabitRecord>): Boolean {
        if (records.size < 14) return false
        
        // Compare last week with previous week
        val sortedRecords = records.sortedBy { it.date }
        val lastWeekRecords = sortedRecords.takeLast(7)
        val previousWeekRecords = sortedRecords.dropLast(7).takeLast(7)
        
        if (lastWeekRecords.isEmpty() || previousWeekRecords.isEmpty()) return false
        
        val lastWeekCompletionRate = lastWeekRecords.count { it.isCompleted }.toFloat() / lastWeekRecords.size
        val previousWeekCompletionRate = previousWeekRecords.count { it.isCompleted }.toFloat() / previousWeekRecords.size
        
        return lastWeekCompletionRate > previousWeekCompletionRate
    }
    
    /**
     * Calculate consistency score (0.0-1.0)
     */
    private fun calculateConsistencyScore(records: List<HabitRecord>): Float {
        if (records.size < 7) return 0f
        
        // Group by day of week
        val dayOfWeekGroups = records.groupBy { it.date.dayOfWeek }
        
        // Calculate completion rate for each day of week
        val dayCompletionRates = dayOfWeekGroups.map { (_, dayRecords) ->
            dayRecords.count { it.isCompleted }.toFloat() / dayRecords.size
        }
        
        // Calculate standard deviation of completion rates
        val mean = dayCompletionRates.average().toFloat()
        val variance = dayCompletionRates.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance)
        
        // Convert to consistency score (lower stdDev = higher consistency)
        return kotlin.math.max(0f, 1f - stdDev)
    }
    
    /**
     * Calculate difficulty trend based on user feedback
     */
    private fun calculateDifficultyTrend(records: List<HabitRecord>): String {
        val recordsWithDifficulty = records.filter { it.isCompleted && it.difficulty != null }
        if (recordsWithDifficulty.size < 5) return "not_enough_data"
        
        // Group by week and calculate average difficulty
        val weeklyDifficulty = recordsWithDifficulty
            .groupBy { record -> 
                record.date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
            }
            .mapValues { (_, records) ->
                records.mapNotNull { it.difficulty }.average().toFloat()
            }
            .toSortedMap()
        
        if (weeklyDifficulty.size < 2) return "not_enough_data"
        
        // Get last two weeks
        val recentWeeks = weeklyDifficulty.entries.takeLast(2)
        val previousDifficulty = recentWeeks.first().value
        val currentDifficulty = recentWeeks.last().value
        
        return when {
            currentDifficulty < previousDifficulty -> "getting_easier"
            currentDifficulty > previousDifficulty -> "getting_harder"
            else -> "stable"
        }
    }
    
    /**
     * Find habits that correlate with this habit
     */
    private suspend fun findCorrelatedHabits(userId: String, habitId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val userHabits = habitRepository.getHabitsByUserId(userId).first()
            if (userHabits.size < 2) return@withContext emptyList()
            
            val targetHabit = userHabits.find { it.id == habitId } ?: return@withContext emptyList()
            val otherHabits = userHabits.filter { it.id != habitId }
            
            // Get records for all habits
            val today = LocalDate.now()
            val startDate = today.minusDays(30) // Look at last 30 days
            
            val targetRecords = habitRepository.getHabitRecordsByDateRange(
                habitId = habitId,
                startDate = startDate,
                endDate = today
            ).first()
            
            val correlatedHabitIds = mutableListOf<String>()
            
            // Check each habit for correlation
            for (otherHabit in otherHabits) {
                val otherRecords = habitRepository.getHabitRecordsByDateRange(
                    habitId = otherHabit.id,
                    startDate = startDate,
                    endDate = today
                ).first()
                
                // Create a map of date to completion status for both habits
                val targetCompletionByDate = targetRecords.associate { it.date to it.isCompleted }
                val otherCompletionByDate = otherRecords.associate { it.date to it.isCompleted }
                
                // Find dates that exist in both records
                val commonDates = targetCompletionByDate.keys.intersect(otherCompletionByDate.keys)
                if (commonDates.size < 7) continue // Need at least a week of common data
                
                // Calculate correlation
                var matchingDays = 0
                for (date in commonDates) {
                    if (targetCompletionByDate[date] == otherCompletionByDate[date]) {
                        matchingDays++
                    }
                }
                
                val correlation = matchingDays.toFloat() / commonDates.size
                if (correlation > 0.7f) {
                    correlatedHabitIds.add(otherHabit.id)
                }
            }
            
            return@withContext correlatedHabitIds
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }    /
**
     * Identify the best time of day for habit completion
     * 
     * @param records List of habit records
     * @return The time of day with highest completion rate, or null if not enough data
     */
    private fun identifyBestTimeOfDay(records: List<HabitRecord>): String? {
        val completedRecords = records.filter { it.isCompleted && it.completionTime != null }
        if (completedRecords.size < 3) return null // Need at least 3 data points
        
        // Group by time of day
        val timeGroups = completedRecords.groupBy { record ->
            val time = record.completionTime!!.toLocalTime()
            when {
                time in morningTimeRange -> "morning"
                time in afternoonTimeRange -> "afternoon"
                time in eveningTimeRange -> "evening"
                else -> "night"
            }
        }
        
        // Find the time of day with highest completion count
        return timeGroups.maxByOrNull { it.value.size }?.key
    }
    
    /**
     * Calculate completion trend based on historical data
     * 
     * @param records List of habit records
     * @return Trend type (IMPROVING, STABLE, DECLINING, FLUCTUATING, or NOT_ENOUGH_DATA)
     */
    private fun calculateCompletionTrend(records: List<HabitRecord>): Trend {
        if (records.size < 7) return Trend.NOT_ENOUGH_DATA
        
        // Group records by week
        val recordsByWeek = records.groupBy { record ->
            record.date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
        }.toSortedMap()
        
        if (recordsByWeek.size < 2) return Trend.NOT_ENOUGH_DATA
        
        // Calculate completion rate for each week
        val weeklyCompletionRates = recordsByWeek.map { (_, weekRecords) ->
            weekRecords.count { it.isCompleted }.toFloat() / weekRecords.size
        }
        
        // Calculate trend
        val recentWeeks = weeklyCompletionRates.takeLast(4)
        if (recentWeeks.size < 2) return Trend.NOT_ENOUGH_DATA
        
        // Check if consistently improving
        val isImproving = recentWeeks.zipWithNext().all { (prev, next) -> next >= prev }
        
        // Check if consistently declining
        val isDeclining = recentWeeks.zipWithNext().all { (prev, next) -> next <= prev }
        
        // Check if stable (within 10% variation)
        val isStable = recentWeeks.zipWithNext().all { (prev, next) -> 
            kotlin.math.abs(next - prev) < 0.1f 
        }
        
        return when {
            isImproving -> Trend.IMPROVING
            isDeclining -> Trend.DECLINING
            isStable -> Trend.STABLE
            else -> Trend.FLUCTUATING
        }
    }
    
    /**
     * Calculate difficulty trend based on user feedback
     * 
     * @param records List of habit records
     * @return Difficulty trend ("getting_easier", "getting_harder", "stable", or "not_enough_data")
     */
    private fun calculateDifficultyTrend(records: List<HabitRecord>): String {
        val recordsWithDifficulty = records.filter { it.isCompleted && it.difficulty != null }
        if (recordsWithDifficulty.size < 5) return "not_enough_data"
        
        // Group by week and calculate average difficulty
        val weeklyDifficulty = recordsWithDifficulty
            .groupBy { record -> 
                record.date.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
            }
            .mapValues { (_, records) ->
                records.mapNotNull { it.difficulty }.average().toFloat()
            }
            .toSortedMap()
        
        if (weeklyDifficulty.size < 2) return "not_enough_data"
        
        // Get last two weeks
        val recentWeeks = weeklyDifficulty.entries.takeLast(2)
        val previousDifficulty = recentWeeks.first().value
        val currentDifficulty = recentWeeks.last().value
        
        return when {
            currentDifficulty < previousDifficulty -> "getting_easier"
            currentDifficulty > previousDifficulty -> "getting_harder"
            else -> "stable"
        }
    }