package com.andychen.habitgem.domain.ai.analysis

import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generator for habit insights based on habit data
 * Implements a template-based system for generating textual insights
 */
class HabitInsightGenerator {
    
    /**
     * Generate a comprehensive insight for a habit
     * @param habit The habit to analyze
     * @param records List of habit records
     * @param patternAnalyzer Optional pattern analyzer to use (will create one if not provided)
     * @return A detailed insight message
     */
    fun generateComprehensiveInsight(
        habit: Habit,
        records: List<HabitRecord>,
        patternAnalyzer: HabitPatternAnalyzer = HabitPatternAnalyzer()
    ): String {
        if (records.isEmpty()) {
            return "您还没有记录这个习惯的数据。开始记录您的习惯，我们将为您提供详细的分析和洞察。"
        }
        
        if (records.size < 7) {
            return "继续记录您的习惯，当您有更多数据时，我们将为您提供更详细的分析和洞察。"
        }
        
        // Analyze patterns
        val patterns = patternAnalyzer.identifyTimeSeriesPatterns(records)
        
        // Generate insights based on patterns
        val insights = mutableListOf<String>()
        
        // Add completion rate insight
        val completionRate = records.count { it.isCompleted }.toFloat() / records.size
        val completionRatePercent = (completionRate * 100).roundToInt()
        
        insights.add(generateCompletionRateInsight(completionRatePercent, habit.name))
        
        // Add streak insight
        val currentStreak = calculateCurrentStreak(records)
        if (abs(currentStreak) >= 3) {
            insights.add(generateStreakInsight(currentStreak, habit.name))
        }
        
        // Add pattern-based insights
        patterns.entries
            .filter { it.value > 0.6f && !it.key.name.startsWith("NO_") && it.key != PatternType.NOT_ENOUGH_DATA }
            .sortedByDescending { it.value }
            .take(2)
            .forEach { (pattern, confidence) ->
                insights.add(generatePatternInsight(pattern, confidence, habit.name))
            }
        
        // Add category-specific insights
        insights.add(generateCategoryInsight(habit.category, completionRate, habit.name))
        
        // Add time-based insights
        val daysSinceStart = ChronoUnit.DAYS.between(habit.startDate, LocalDate.now()) + 1
        if (daysSinceStart > 30) {
            insights.add(generateLongTermInsight(daysSinceStart.toInt(), completionRatePercent, habit.name))
        }
        
        // Combine insights into a comprehensive message
        return insights.joinToString("\\n\\n")
    }
    
    /**
     * Generate a short insight for a habit (for UI display)
     * @param habit The habit to analyze
     * @param records List of habit records
     * @return A short insight message
     */
    fun generateShortInsight(habit: Habit, records: List<HabitRecord>): String {
        if (records.isEmpty()) {
            return "开始记录您的习惯，获取个性化洞察。"
        }
        
        if (records.size < 5) {
            return "继续记录您的习惯，很快就会有更多洞察。"
        }
        
        // Calculate basic metrics
        val completionRate = records.count { it.isCompleted }.toFloat() / records.size
        val completionRatePercent = (completionRate * 100).roundToInt()
        val currentStreak = calculateCurrentStreak(records)
        
        // Generate a short insight based on the most relevant metric
        return when {
            abs(currentStreak) >= 5 -> {
                if (currentStreak > 0) {
                    "您已连续完成这个习惯$currentStreak天！继续保持！"
                } else {
                    "您已连续${abs(currentStreak)}天未完成这个习惯。今天是重新开始的好时机！"
                }
            }
            completionRate > 0.8 -> {
                "出色的表现！您的完成率达到了${completionRatePercent}%。"
            }
            completionRate < 0.3 -> {
                "这个习惯对您来说可能有些挑战。考虑调整难度或设置提醒。"
            }
            else -> {
                val recentRecords = records.sortedByDescending { it.date }.take(7)
                val recentCompletionRate = recentRecords.count { it.isCompleted }.toFloat() / recentRecords.size
                
                if (recentCompletionRate > completionRate * 1.2) {
                    "最近一周您的表现有所提升，继续保持这个势头！"
                } else if (recentCompletionRate < completionRate * 0.8) {
                    "最近一周您的完成率有所下降，找回您的动力！"
                } else {
                    "您的习惯完成率为${completionRatePercent}%，保持稳定。"
                }
            }
        }
    }
    
    /**
     * Generate a periodic report for multiple habits
     * @param habits Map of habits to their records
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return A report with insights and recommendations
     */
    fun generatePeriodicReport(
        habits: Map<Habit, List<HabitRecord>>,
        startDate: LocalDate,
        endDate: LocalDate
    ): PeriodicReportInsight {
        if (habits.isEmpty()) {
            return PeriodicReportInsight(
                summary = "您还没有添加任何习惯。开始添加习惯，我们将为您提供详细的分析和洞察。",
                insights = emptyList(),
                recommendations = listOf("尝试添加一个简单的每日习惯，如冥想5分钟或喝8杯水。")
            )
        }
        
        val dateFormatter = DateTimeFormatter.ofPattern("MM月dd日")
        val periodDescription = "${startDate.format(dateFormatter)}至${endDate.format(dateFormatter)}"
        
        // Calculate overall completion rate
        val allRecords = habits.values.flatten()
        val periodRecords = allRecords.filter { it.date in startDate..endDate }
        
        val overallCompletionRate = if (periodRecords.isNotEmpty()) {
            periodRecords.count { it.isCompleted }.toFloat() / periodRecords.size
        } else 0f
        
        val overallCompletionPercent = (overallCompletionRate * 100).roundToInt()
        
        // Generate summary
        val summary = when {
            periodRecords.isEmpty() -> 
                "在$periodDescription期间，您没有任何习惯记录。"
            overallCompletionRate > 0.8 -> 
                "在$periodDescription期间，您的习惯完成率达到了${overallCompletionPercent}%，表现非常出色！"
            overallCompletionRate > 0.5 -> 
                "在$periodDescription期间，您的习惯完成率为${overallCompletionPercent}%，表现不错。"
            else -> 
                "在$periodDescription期间，您的习惯完成率为${overallCompletionPercent}%，还有提升空间。"
        }
        
        // Generate insights for each habit
        val habitInsights = mutableListOf<String>()
        
        // Sort habits by completion rate
        val sortedHabits = habits.entries
            .filter { (_, records) -> records.any { it.date in startDate..endDate } }
            .sortedByDescending { (_, records) -> 
                val relevantRecords = records.filter { it.date in startDate..endDate }
                if (relevantRecords.isNotEmpty()) {
                    relevantRecords.count { it.isCompleted }.toFloat() / relevantRecords.size
                } else 0f
            }
        
        // Add insights for top and bottom performing habits
        if (sortedHabits.isNotEmpty()) {
            val topHabit = sortedHabits.first()
            val topHabitRecords = topHabit.value.filter { it.date in startDate..endDate }
            if (topHabitRecords.isNotEmpty()) {
                val topCompletionRate = topHabitRecords.count { it.isCompleted }.toFloat() / topHabitRecords.size
                val topCompletionPercent = (topCompletionRate * 100).roundToInt()
                
                habitInsights.add("您在"${topHabit.key.name}"习惯上表现最佳，完成率达到了${topCompletionPercent}%。")
            }
            
            if (sortedHabits.size > 1) {
                val bottomHabit = sortedHabits.last()
                val bottomHabitRecords = bottomHabit.value.filter { it.date in startDate..endDate }
                if (bottomHabitRecords.isNotEmpty()) {
                    val bottomCompletionRate = bottomHabitRecords.count { it.isCompleted }.toFloat() / bottomHabitRecords.size
                    val bottomCompletionPercent = (bottomCompletionRate * 100).roundToInt()
                    
                    if (bottomCompletionRate < 0.5) {
                        habitInsights.add("您在"${bottomHabit.key.name}"习惯上遇到了一些挑战，完成率为${bottomCompletionPercent}%。考虑调整这个习惯的难度或频率。")
                    }
                }
            }
        }
        
        // Add trend insight
        val firstHalfRecords = periodRecords.filter { 
            it.date.isBefore(startDate.plusDays(ChronoUnit.DAYS.between(startDate, endDate) / 2)) 
        }
        val secondHalfRecords = periodRecords.filter { 
            !firstHalfRecords.contains(it) 
        }
        
        if (firstHalfRecords.isNotEmpty() && secondHalfRecords.isNotEmpty()) {
            val firstHalfRate = firstHalfRecords.count { it.isCompleted }.toFloat() / firstHalfRecords.size
            val secondHalfRate = secondHalfRecords.count { it.isCompleted }.toFloat() / secondHalfRecords.size
            
            if (secondHalfRate > firstHalfRate * 1.2) {
                habitInsights.add("您在这段时间的后半期表现明显提升，继续保持这个良好趋势！")
            } else if (secondHalfRate < firstHalfRate * 0.8) {
                habitInsights.add("您在这段时间的后半期完成率有所下降，可能需要重新找回动力。")
            }
        }
        
        // Generate recommendations
        val recommendations = mutableListOf<String>()
        
        // Recommend based on completion patterns
        val dayCompletions = periodRecords
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, records) -> 
                records.count { it.isCompleted }.toFloat() / records.size 
            }
        
        if (dayCompletions.isNotEmpty()) {
            val bestDay = dayCompletions.maxByOrNull { it.value }?.key
            val worstDay = dayCompletions.minByOrNull { it.value }?.key
            
            if (bestDay != null && worstDay != null && bestDay != worstDay) {
                recommendations.add("您在${bestDay.name}的表现最佳，而在${worstDay.name}的表现最差。考虑调整您在${worstDay.name}的习惯安排或设置额外提醒。")
            }
        }
        
        // Recommend based on habit categories
        val categoryCompletions = habits.entries
            .flatMap { (habit, records) -> 
                records.filter { it.date in startDate..endDate }
                    .map { habit.category to it.isCompleted } 
            }
            .groupBy { it.first }
            .mapValues { (_, pairs) -> 
                pairs.count { it.second }.toFloat() / pairs.size 
            }
        
        if (categoryCompletions.size > 1) {
            val bestCategory = categoryCompletions.maxByOrNull { it.value }?.key
            val worstCategory = categoryCompletions.minByOrNull { it.value }?.key
            
            if (bestCategory != null && worstCategory != null && bestCategory != worstCategory) {
                recommendations.add("您在${bestCategory.name}类习惯上表现最佳，而在${worstCategory.name}类习惯上遇到了更多挑战。考虑如何将您在${bestCategory.name}类习惯上的成功策略应用到${worstCategory.name}类习惯上。")
            }
        }
        
        // Add general recommendation if needed
        if (recommendations.isEmpty()) {
            if (overallCompletionRate < 0.5) {
                recommendations.add("考虑减少您正在追踪的习惯数量，专注于最重要的几个习惯，或降低一些习惯的难度。")
            } else {
                recommendations.add("您的习惯养成进展良好。考虑为自己设定一个新的挑战，或提高现有习惯的难度。")
            }
        }
        
        return PeriodicReportInsight(
            summary = summary,
            insights = habitInsights,
            recommendations = recommendations
        )
    }
    
    /**
     * Generate insight based on completion rate
     */
    private fun generateCompletionRateInsight(completionRatePercent: Int, habitName: String): String {
        return when {
            completionRatePercent > 90 -> "您的"$habitName"习惯完成率高达${completionRatePercent}%，这是非常出色的表现！坚持这个习惯已经成为您生活的一部分。"
            completionRatePercent > 75 -> "您的"$habitName"习惯完成率为${completionRatePercent}%，表现很好！您已经建立了稳定的习惯模式。"
            completionRatePercent > 50 -> "您的"$habitName"习惯完成率为${completionRatePercent}%，超过了一半的时间。继续努力，保持一致性是关键。"
            completionRatePercent > 30 -> "您的"$habitName"习惯完成率为${completionRatePercent}%。这个习惯对您来说可能有些挑战，考虑调整难度或设置提醒。"
            else -> "您的"$habitName"习惯完成率为${completionRatePercent}%，看起来您在坚持这个习惯上遇到了困难。考虑重新评估这个习惯的难度或相关性。"
        }
    }
    
    /**
     * Generate insight based on streak
     */
    private fun generateStreakInsight(streak: Int, habitName: String): String {
        return when {
            streak > 30 -> "令人印象深刻！您已经连续${streak}天完成"$habitName"习惯。这样的坚持展示了您的非凡毅力！"
            streak > 21 -> "出色的坚持！您已经连续${streak}天完成"$habitName"习惯。研究表明，21天是形成习惯的关键期，您已经成功跨越这个门槛！"
            streak > 14 -> "很棒的连续性！您已经连续${streak}天完成"$habitName"习惯。保持这个势头，您正在建立一个强大的习惯基础。"
            streak > 7 -> "好的开始！您已经连续${streak}天完成"$habitName"习惯。第一周是最困难的，您已经成功度过了这个阶段。"
            streak > 3 -> "您已经连续${streak}天完成"$habitName"习惯。每一天的坚持都在帮助这个习惯扎根。"
            streak < -14 -> "您已经连续${abs(streak)}天未完成"$habitName"习惯。这可能是重新评估这个习惯的好时机。它是否仍然符合您的目标？"
            streak < -7 -> "您已经连续${abs(streak)}天未完成"$habitName"习惯。考虑是什么阻碍了您，也许需要调整习惯的难度或时间安排。"
            streak < -3 -> "您已经连续${abs(streak)}天未完成"$habitName"习惯。今天是重新开始的好时机！"
            else -> "您的"$habitName"习惯连续性一般，尝试建立更长的完成连续记录可以增强习惯的形成。"
        }
    }
    
    /**
     * Generate insight based on pattern type
     */
    private fun generatePatternInsight(pattern: PatternType, confidence: Float, habitName: String): String {
        val confidenceStr = when {
            confidence > 0.9f -> "非常明显"
            confidence > 0.7f -> "明显"
            else -> "一定"
        }
        
        return when (pattern) {
            PatternType.WEEKDAY_PREFERENCE -> 
                "数据显示，您在工作日完成"$habitName"习惯的表现$confidenceStr优于周末。工作日的规律可能对您的习惯养成有积极影响。"
                
            PatternType.WEEKEND_PREFERENCE -> 
                "数据显示，您在周末完成"$habitName"习惯的表现$confidenceStr优于工作日。周末的灵活时间可能对您的习惯养成有积极影响。"
                
            PatternType.MORNING_PREFERENCE -> 
                "数据显示，您在早晨完成"$habitName"习惯的成功率$confidenceStr高于其他时间段。早晨的时间可能是您执行这个习惯的最佳时机。"
                
            PatternType.AFTERNOON_PREFERENCE -> 
                "数据显示，您在下午完成"$habitName"习惯的成功率$confidenceStr高于其他时间段。下午的时间可能是您执行这个习惯的最佳时机。"
                
            PatternType.EVENING_PREFERENCE -> 
                "数据显示，您在晚上完成"$habitName"习惯的成功率$confidenceStr高于其他时间段。晚上的时间可能是您执行这个习惯的最佳时机。"
                
            PatternType.NIGHT_PREFERENCE -> 
                "数据显示，您在夜间完成"$habitName"习惯的成功率$confidenceStr高于其他时间段。夜间的时间可能是您执行这个习惯的最佳时机。"
                
            PatternType.IMPROVING_TREND -> 
                "令人鼓舞的是，您的"$habitName"习惯完成率呈现$confidenceStr的上升趋势。您正在建立越来越强的习惯模式。"
                
            PatternType.DECLINING_TREND -> 
                "数据显示，您的"$habitName"习惯完成率近期有$confidenceStr的下降趋势。这可能是重新评估和调整的好时机。"
                
            PatternType.STABLE_TREND -> 
                "您的"$habitName"习惯完成率保持$confidenceStr的稳定。稳定性是习惯养成的重要特征，这是个好迹象。"
                
            PatternType.FLUCTUATING_TREND -> 
                "您的"$habitName"习惯完成情况显示出$confidenceStr的波动性。尝试找出影响您一致性的因素可能会有所帮助。"
                
            PatternType.STREAK_BASED -> 
                "您的"$habitName"习惯表现出$confidenceStr的连续性模式。对您来说，保持连续记录似乎是一个有效的动力来源。"
                
            PatternType.SPECIFIC_DAY_PATTERN -> 
                "数据显示，您在特定几天完成"$habitName"习惯的表现$confidenceStr优于其他日子。了解这种模式可以帮助您更好地安排习惯。"
                
            else -> 
                "您的"$habitName"习惯数据显示出一些有趣的模式。随着更多数据的积累，我们将能提供更精确的分析。"
        }
    }
    
    /**
     * Generate insight based on habit category
     */
    private fun generateCategoryInsight(category: HabitCategory, completionRate: Float, habitName: String): String {
        val categorySpecificInsight = when (category) {
            HabitCategory.HEALTH -> 
                if (completionRate > 0.7) 
                    "坚持"$habitName"这样的健康习惯对您的整体健康有积极影响。研究表明，健康习惯的累积效应会随着时间推移而增强。"
                else
                    "健康习惯如"$habitName"需要时间才能看到明显效果，但每次完成都是对健康的投资。"
                    
            HabitCategory.FITNESS -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的健身习惯的坚持令人印象深刻。持续的体育活动不仅改善身体健康，还能提升心理健康。"
                else
                    "健身习惯如"$habitName"可能需要额外的动力。考虑设定小目标或找一个伙伴一起坚持。"
                    
            HabitCategory.MINDFULNESS -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的正念习惯的坚持非常好。研究表明，正念练习可以减轻压力，提高专注力和情绪稳定性。"
                else
                    "正念习惯如"$habitName"需要耐心和一致性。即使是短暂的练习也能带来益处。"
                    
            HabitCategory.PRODUCTIVITY -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的生产力习惯的坚持非常出色。这些习惯的累积效应可以显著提高您的整体效率和成就感。"
                else
                    "生产力习惯如"$habitName"可能需要更好的整合到您的日常工作流程中。考虑将其与现有的日常活动结合。"
                    
            HabitCategory.LEARNING -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的学习习惯的坚持令人印象深刻。持续学习是个人成长和适应能力的关键。"
                else
                    "学习习惯如"$habitName"需要持续的投入。即使每天只投入少量时间，长期坚持也会带来显著成果。"
                    
            HabitCategory.SOCIAL -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的社交习惯的坚持非常好。积极的社交互动对心理健康和整体幸福感有重要影响。"
                else
                    "社交习惯如"$habitName"可能需要更多的计划和准备。考虑设定具体的社交目标或活动。"
                    
            HabitCategory.CREATIVITY -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的创造性习惯的坚持非常出色。定期的创造性活动可以增强问题解决能力和心理弹性。"
                else
                    "创造性习惯如"$habitName"需要灵感和动力。尝试在不同环境中进行这个习惯，或寻找新的灵感来源。"
                    
            HabitCategory.FINANCE -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"这样的财务习惯的坚持非常好。良好的财务习惯是长期财务健康和安全的基础。"
                else
                    "财务习惯如"$habitName"需要纪律和一致性。考虑设置自动提醒或与您的其他日常活动结合。"
                    
            else -> 
                if (completionRate > 0.7) 
                    "您对"$habitName"习惯的坚持非常出色。持续的努力是任何成功习惯养成的关键。"
                else
                    "坚持"$habitName"习惯可能需要找到更适合您的方法。考虑调整时间、难度或设置更明确的目标。"
        }
        
        return categorySpecificInsight
    }
    
    /**
     * Generate insight based on long-term habit tracking
     */
    private fun generateLongTermInsight(daysSinceStart: Int, completionRatePercent: Int, habitName: String): String {
        val monthsSinceStart = daysSinceStart / 30
        
        return when {
            monthsSinceStart >= 6 && completionRatePercent > 70 ->
                "您已经坚持"$habitName"习惯超过${monthsSinceStart}个月，并保持了${completionRatePercent}%的高完成率。这展示了非凡的毅力和自律，这个习惯已经成为您生活的重要部分。"
                
            monthsSinceStart >= 6 ->
                "您已经追踪"$habitName"习惯超过${monthsSinceStart}个月，完成率为${completionRatePercent}%。长期坚持一个习惯是很有挑战性的，您的持续努力值得赞赏。"
                
            monthsSinceStart >= 3 && completionRatePercent > 70 ->
                "您已经坚持"$habitName"习惯超过${monthsSinceStart}个月，并保持了${completionRatePercent}%的高完成率。研究表明，3个月是习惯真正融入生活方式的重要里程碑。"
                
            monthsSinceStart >= 3 ->
                "您已经追踪"$habitName"习惯超过${monthsSinceStart}个月，完成率为${completionRatePercent}%。三个月是一个重要的里程碑，表明您对这个习惯有长期的承诺。"
                
            monthsSinceStart >= 1 && completionRatePercent > 70 ->
                "您已经坚持"$habitName"习惯超过${monthsSinceStart}个月，并保持了${completionRatePercent}%的高完成率。第一个月通常是最困难的，您已经成功度过了这个关键期。"
                
            monthsSinceStart >= 1 ->
                "您已经追踪"$habitName"习惯超过${monthsSinceStart}个月，完成率为${completionRatePercent}%。继续坚持，习惯的形成需要时间和一致性。"
                
            else ->
                "您已经开始追踪"$habitName"习惯${daysSinceStart}天，完成率为${completionRatePercent}%。好的开始是成功的一半，继续保持一致性。"
        }
    }
    
    /**
     * Calculate the current streak (positive for completion streak, negative for missing streak)
     */
    private fun calculateCurrentStreak(records: List<HabitRecord>): Int {
        if (records.isEmpty()) return 0
        
        val sortedRecords = records.sortedByDescending { it.date }
        var streak = 0
        val firstCompleted = sortedRecords.first().isCompleted
        
        for (record in sortedRecords) {
            if (record.isCompleted == firstCompleted) {
                streak += if (firstCompleted) 1 else -1
            } else {
                break
            }
        }
        
        return streak
    }
}

/**
 * Data class for periodic report insights
 */
data class PeriodicReportInsight(
    val summary: String,
    val insights: List<String>,
    val recommendations: List<String>
)