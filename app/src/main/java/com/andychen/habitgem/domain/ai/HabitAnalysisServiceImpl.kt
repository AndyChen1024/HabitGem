package com.andychen.habitgem.domain.ai

import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.model.HabitAnalysisRequest
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.domain.ai.analysis.HabitInsightGenerator
import com.andychen.habitgem.domain.ai.analysis.HabitPatternAnalyzer
import com.andychen.habitgem.domain.ai.analysis.PatternType
import com.andychen.habitgem.domain.ai.analysis.PeriodicReportInsight
import com.andychen.habitgem.domain.model.CorrelationType
import com.andychen.habitgem.domain.model.DataPoint
import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCorrelation
import com.andychen.habitgem.domain.model.HabitInsight
import com.andychen.habitgem.domain.model.HabitRecord
import com.andychen.habitgem.domain.model.OptimizationSuggestion
import com.andychen.habitgem.domain.model.PeriodicReport
import com.andychen.habitgem.domain.model.ReportPeriod
import com.andychen.habitgem.domain.model.SuggestionType
import com.andychen.habitgem.domain.model.Trend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Implementation of HabitAnalysisService
 * Uses advanced pattern recognition algorithms and insight generation to analyze habit data
 */
class HabitAnalysisServiceImpl(
    private val aiServiceApi: AIServiceApi,
    private val habitRepository: HabitRepository
) : HabitAnalysisService {
    
    private val patternAnalyzer = HabitPatternAnalyzer()
    private val insightGenerator = HabitInsightGenerator()
    
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
    
    /**
     * Get a periodic report for a user's habits
     * @param userId User ID
     * @param period Report period (DAILY, WEEKLY, MONTHLY)
     * @return Periodic report with insights and recommendations
     */
    suspend fun getPeriodicReport(userId: String, period: ReportPeriod): PeriodicReport = withContext(Dispatchers.IO) {
        try {
            // Calculate date range based on period
            val endDate = LocalDate.now()
            val startDate = when (period) {
                ReportPeriod.DAILY -> endDate
                ReportPeriod.WEEKLY -> endDate.minusDays(6) // Last 7 days
                ReportPeriod.MONTHLY -> endDate.minusDays(29) // Last 30 days
            }
            
            // Get all habits for the user
            val habits = habitRepository.getHabitsByUserId(userId).first()
            
            // Get records for each habit in the date range
            val habitRecords = habits.associateWith { habit ->
                habitRepository.getHabitRecordsByDateRange(userId, startDate, endDate).first()
                    .filter { it.habitId == habit.id }
            }
            
            // Generate report using insight generator
            val reportInsight = insightGenerator.generatePeriodicReport(habitRecords, startDate, endDate)
            
            // Calculate completion rates for each habit
            val habitCompletionRates = habitRecords.mapValues { (_, records) ->
                if (records.isNotEmpty()) {
                    records.count { it.isCompleted }.toFloat() / records.size
                } else 0f
            }
            
            // Calculate overall completion rate
            val allRecords = habitRecords.values.flatten()
            val overallCompletionRate = if (allRecords.isNotEmpty()) {
                allRecords.count { it.isCompleted }.toFloat() / allRecords.size
            } else 0f
            
            // Generate visual data points for the report
            val visualData = generateVisualDataForPeriod(habitRecords, startDate, endDate)
            
            return@withContext PeriodicReport(
                period = period,
                startDate = startDate,
                endDate = endDate,
                completionRate = overallCompletionRate,
                habitsSummary = habitCompletionRates.mapKeys { it.key.id },
                insights = reportInsight.insights,
                recommendations = reportInsight.recommendations
            )
        } catch (e: Exception) {
            // Return a basic report if there's an error
            return@withContext PeriodicReport(
                period = period,
                startDate = LocalDate.now().minusDays(when(period) {
                    ReportPeriod.DAILY -> 0
                    ReportPeriod.WEEKLY -> 6
                    ReportPeriod.MONTHLY -> 29
                }),
                endDate = LocalDate.now(),
                completionRate = 0f,
                habitsSummary = emptyMap(),
                insights = listOf("无法生成报告，请稍后再试。"),
                recommendations = listOf("继续记录您的习惯，我们将为您提供更详细的分析。")
            )
        }
    }
    
    /**
     * Generate visual data points for a period report
     */
    private fun generateVisualDataForPeriod(
        habitRecords: Map<Habit, List<HabitRecord>>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DataPoint> {
        // For daily reports, return empty list (no visualization needed)
        if (startDate == endDate) {
            return emptyList()
        }
        
        val dataPoints = mutableListOf<DataPoint>()
        
        // For weekly reports, generate daily completion rates
        if (ChronoUnit.DAYS.between(startDate, endDate) <= 7) {
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dayRecords = habitRecords.values.flatten().filter { it.date == currentDate }
                val completionRate = if (dayRecords.isNotEmpty()) {
                    dayRecords.count { it.isCompleted }.toFloat() / dayRecords.size
                } else 0f
                
                dataPoints.add(
                    DataPoint(
                        date = currentDate,
                        value = completionRate,
                        label = currentDate.dayOfWeek.name
                    )
                )
                
                currentDate = currentDate.plusDays(1)
            }
        } 
        // For monthly reports, generate weekly completion rates
        else {
            // Split the date range into weeks
            val weeks = mutableListOf<Pair<LocalDate, LocalDate>>()
            var weekStart = startDate
            
            while (!weekStart.isAfter(endDate)) {
                val weekEnd = minOf(weekStart.plusDays(6), endDate)
                weeks.add(weekStart to weekEnd)
                weekStart = weekEnd.plusDays(1)
            }
            
            // Calculate completion rate for each week
            weeks.forEach { (weekStart, weekEnd) ->
                val weekRecords = habitRecords.values.flatten()
                    .filter { it.date in weekStart..weekEnd }
                
                val completionRate = if (weekRecords.isNotEmpty()) {
                    weekRecords.count { it.isCompleted }.toFloat() / weekRecords.size
                } else 0f
                
                dataPoints.add(
                    DataPoint(
                        date = weekStart,
                        value = completionRate,
                        label = "${weekStart.monthValue}/${weekStart.dayOfMonth}-${weekEnd.monthValue}/${weekEnd.dayOfMonth}"
                    )
                )
            }
        }
        
        return dataPoints
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
            // Get all habits for the user
            val habits = habitRepository.getHabitsByUserId(userId).first()
            
            // We need at least 2 habits to find correlations
            if (habits.size < 2) {
                return@withContext emptyList()
            }
            
            // Get records for each habit
            val habitRecords = habits.associateWith { habit ->
                habitRepository.getHabitRecords(habit.id).first()
            }
            
            // Filter habits with enough records for analysis
            val habitsWithRecords = habitRecords.filter { it.value.size >= 7 }
            
            // If we don't have enough data, return empty list
            if (habitsWithRecords.size < 2) {
                return@withContext emptyList()
            }
            
            // Calculate correlations between habits
            val correlations = mutableListOf<HabitCorrelation>()
            
            // For each pair of habits
            for (i in habitsWithRecords.keys.toList().indices) {
                for (j in i + 1 until habitsWithRecords.keys.toList().size) {
                    val habit1 = habitsWithRecords.keys.toList()[i]
                    val habit2 = habitsWithRecords.keys.toList()[j]
                    
                    val records1 = habitsWithRecords[habit1] ?: continue
                    val records2 = habitsWithRecords[habit2] ?: continue
                    
                    // Calculate correlation coefficient
                    val strength = calculateCorrelation(records1, records2)
                    
                    // Determine correlation type
                    val correlationType = when {
                        strength > 0.3f -> CorrelationType.POSITIVE
                        strength < -0.3f -> CorrelationType.NEGATIVE
                        else -> CorrelationType.NEUTRAL
                    }
                    
                    // Generate description based on correlation type and strength
                    val description = when {
                        strength > 0.7f -> 
                            "完成${habit1.name}后，您几乎总是会完成${habit2.name}。这两个习惯有很强的正相关性。"
                        strength > 0.3f -> 
                            "完成${habit1.name}后，您更有可能完成${habit2.name}。考虑将这两个习惯组合在一起。"
                        strength < -0.7f -> 
                            "在完成${habit1.name}的日子里，您几乎从不完成${habit2.name}。这两个习惯有很强的负相关性。"
                        strength < -0.3f -> 
                            "在完成${habit1.name}的日子里，您较少完成${habit2.name}。考虑在不同的日子安排这两个习惯。"
                        else -> 
                            "这两个习惯之间没有明显的关联。它们似乎是相互独立的。"
                    }
                    
                    // Add correlation to list
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
            
            // Return only significant correlations
            return@withContext correlations.filter { 
                Math.abs(it.correlationStrength) > 0.3f 
            }.sortedByDescending { 
                Math.abs(it.correlationStrength) 
            }
        } catch (e: Exception) {
            // Return empty list if there's an error
            return@withContext emptyList()
        }
    }
    
    /**
     * Calculate correlation coefficient between two habits based on their completion records
     * Uses phi coefficient (similar to Pearson correlation for binary data)
     */
    private fun calculateCorrelation(records1: List<HabitRecord>, records2: List<HabitRecord>): Float {
        // Group records by date
        val recordsByDate1 = records1.associateBy { it.date }
        val recordsByDate2 = records2.associateBy { it.date }
        
        // Find common dates
        val commonDates = recordsByDate1.keys.intersect(recordsByDate2.keys)
        
        // If we don't have enough common dates, return 0
        if (commonDates.size < 5) {
            return 0f
        }
        
        // Count occurrences for phi coefficient calculation
        var n11 = 0 // Both habits completed
        var n10 = 0 // Habit1 completed, Habit2 not completed
        var n01 = 0 // Habit1 not completed, Habit2 completed
        var n00 = 0 // Neither habit completed
        
        for (date in commonDates) {
            val completed1 = recordsByDate1[date]?.isCompleted ?: false
            val completed2 = recordsByDate2[date]?.isCompleted ?: false
            
            when {
                completed1 && completed2 -> n11++
                completed1 && !completed2 -> n10++
                !completed1 && completed2 -> n01++
                !completed1 && !completed2 -> n00++
            }
        }
        
        // Calculate phi coefficient
        val numerator = (n11 * n00 - n10 * n01).toFloat()
        val denominator = sqrt(
            (n11 + n10).toFloat() * 
            (n01 + n00).toFloat() * 
            (n11 + n01).toFloat() * 
            (n10 + n00).toFloat()
        )
        
        return if (denominator > 0) numerator / denominator else 0f
    }
    
    /**
     * Square root function for Float
     */
    private fun sqrt(value: Float): Float {
        return Math.sqrt(value.toDouble()).toFloat()
    }
    
    /**
     * Generate local insights based on habit data using pattern recognition and insight generator
     */
    private suspend fun generateLocalInsights(userId: String, habitId: String): HabitInsight {
        // Get habit and records
        val habit = habitRepository.getHabitById(habitId).first() ?: return HabitInsight(
            habitId = habitId,
            bestPerformingDays = emptyList(),
            completionTrend = Trend.NOT_ENOUGH_DATA,
            consistencyScore = 0f,
            insightMessage = "无法找到该习惯的数据。"
        )
        
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
        
        // Use pattern analyzer to identify patterns
        val patterns = patternAnalyzer.identifyTimeSeriesPatterns(records)
        
        // Determine best performing days using pattern analysis
        val bestDays = when {
            patterns.containsKey(PatternType.SPECIFIC_DAY_PATTERN) -> {
                // Calculate best performing days
                records
                    .filter { it.isCompleted }
                    .groupBy { it.date.dayOfWeek }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(2)
                    .map { it.key }
            }
            patterns.containsKey(PatternType.WEEKDAY_PREFERENCE) -> {
                listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                       DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                    .sortedByDescending { day ->
                        records.filter { it.isCompleted && it.date.dayOfWeek == day }.size
                    }
                    .take(2)
            }
            patterns.containsKey(PatternType.WEEKEND_PREFERENCE) -> {
                listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            }
            else -> {
                // Fallback to simple calculation
                records
                    .filter { it.isCompleted }
                    .groupBy { it.date.dayOfWeek }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .take(1)
                    .map { it.key }
            }
        }
        
        // Determine completion trend from pattern analysis
        val trend = when {
            patterns.containsKey(PatternType.IMPROVING_TREND) -> Trend.IMPROVING
            patterns.containsKey(PatternType.DECLINING_TREND) -> Trend.DECLINING
            patterns.containsKey(PatternType.STABLE_TREND) -> Trend.STABLE
            patterns.containsKey(PatternType.FLUCTUATING_TREND) -> Trend.FLUCTUATING
            else -> Trend.NOT_ENOUGH_DATA
        }
        
        // Calculate consistency score using more advanced metrics
        val consistencyScore = if (records.size >= 7) {
            // Check for anomalies
            val anomalies = patternAnalyzer.detectAnomalies(records)
            val anomalyRate = if (records.isNotEmpty()) {
                anomalies.size.toFloat() / records.size
            } else 0f
            
            // Base score is completion rate
            val baseScore = records.count { it.isCompleted }.toFloat() / records.size
            
            // Adjust for anomalies (fewer anomalies = more consistent)
            val adjustedScore = baseScore * (1f - anomalyRate * 0.5f)
            
            // Adjust for streak patterns (higher streak confidence = more consistent)
            val streakConfidence = patterns[PatternType.STREAK_BASED] ?: 0f
            val finalScore = adjustedScore * (1f + streakConfidence * 0.3f)
            
            finalScore.coerceIn(0f, 1f)
        } else {
            records.count { it.isCompleted }.toFloat() / records.size
        }
        
        // Generate comprehensive insight using the insight generator
        val insightMessage = insightGenerator.generateComprehensiveInsight(habit, records, patternAnalyzer)
        
        return HabitInsight(
            habitId = habitId,
            bestPerformingDays = bestDays,
            completionTrend = trend,
            consistencyScore = consistencyScore,
            insightMessage = insightMessage
        )
    }
    
    /**
     * Generate local optimization suggestions using pattern recognition
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
        
        // Use pattern analyzer to identify patterns
        val patterns = patternAnalyzer.identifyTimeSeriesPatterns(records)
        
        // Detect anomalies in habit records
        val anomalies = patternAnalyzer.detectAnomalies(records)
        
        // Predict future completion probability for next 7 days
        val today = LocalDate.now()
        val futurePredictions = (0..6).associate { 
            today.plusDays(it.toLong()) to patternAnalyzer.predictCompletionProbability(records, today.plusDays(it.toLong()))
        }
        
        // Cluster habit records to find behavior patterns
        val clusters = patternAnalyzer.clusterHabitRecords(records)
        
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        // Calculate completion rate
        val completionRate = records.count { it.isCompleted }.toFloat() / records.size
        
        // Add suggestions based on pattern analysis
        
        // 1. Suggestions based on time patterns
        if (patterns.containsKey(PatternType.MORNING_PREFERENCE) && patterns[PatternType.MORNING_PREFERENCE]!! > 0.7) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.TIME_CHANGE,
                    message = "您在早晨完成习惯的成功率最高，建议继续在早晨安排这个习惯。",
                    expectedImpact = "可能保持或提高当前${(completionRate * 100).toInt()}%的完成率",
                    confidence = patterns[PatternType.MORNING_PREFERENCE]!!
                )
            )
        } else if (patterns.containsKey(PatternType.EVENING_PREFERENCE) && patterns[PatternType.EVENING_PREFERENCE]!! > 0.7) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.TIME_CHANGE,
                    message = "您在晚上完成习惯的成功率最高，建议继续在晚上安排这个习惯。",
                    expectedImpact = "可能保持或提高当前${(completionRate * 100).toInt()}%的完成率",
                    confidence = patterns[PatternType.EVENING_PREFERENCE]!!
                )
            )
        }
        
        // 2. Suggestions based on weekly patterns
        if (patterns.containsKey(PatternType.WEEKDAY_PREFERENCE) && patterns[PatternType.WEEKDAY_PREFERENCE]!! > 0.7) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.FREQUENCY_ADJUST,
                    message = "您在工作日完成习惯的表现明显优于周末，考虑将这个习惯设置为仅工作日执行。",
                    expectedImpact = "可能提高整体完成率10-15%",
                    confidence = patterns[PatternType.WEEKDAY_PREFERENCE]!!
                )
            )
        } else if (patterns.containsKey(PatternType.WEEKEND_PREFERENCE) && patterns[PatternType.WEEKEND_PREFERENCE]!! > 0.7) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.FREQUENCY_ADJUST,
                    message = "您在周末完成习惯的表现明显优于工作日，考虑将这个习惯设置为仅周末执行，或在工作日降低难度。",
                    expectedImpact = "可能提高整体完成率10-15%",
                    confidence = patterns[PatternType.WEEKEND_PREFERENCE]!!
                )
            )
        }
        
        // 3. Suggestions based on completion rate and trend
        if (completionRate < 0.5 && patterns.containsKey(PatternType.DECLINING_TREND)) {
            // If completion rate is low and declining, suggest adjusting difficulty
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.DIFFICULTY_ADJUST,
                    message = "这个习惯的完成率较低且呈下降趋势，强烈建议降低难度或减少频率。",
                    expectedImpact = "可能提高完成率25-35%",
                    confidence = 0.9f
                )
            )
        } else if (completionRate < 0.5) {
            // If just low completion rate
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.DIFFICULTY_ADJUST,
                    message = "这个习惯的完成率较低，考虑降低难度或减少频率。",
                    expectedImpact = "可能提高完成率20-30%",
                    confidence = 0.8f
                )
            )
        }
        
        // 4. Suggestions based on anomalies
        if (anomalies.isNotEmpty()) {
            // Find days with anomalies
            val anomalyDays = anomalies.map { it.first.date.dayOfWeek }
                .groupBy { it }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(2)
                .map { it.key }
                
            if (anomalyDays.isNotEmpty()) {
                suggestions.add(
                    OptimizationSuggestion(
                        type = SuggestionType.TIME_CHANGE,
                        message = "您在${anomalyDays.joinToString("和") { it.name }}的习惯完成情况异常波动，考虑为这些天制定特别策略。",
                        expectedImpact = "可能减少习惯执行的不确定性",
                        confidence = 0.75f
                    )
                )
            }
        }
        
        // 5. Suggestions based on future predictions
        val lowProbabilityDays = futurePredictions.filter { it.value < 0.4 }
            .keys
            .map { it.dayOfWeek }
            .distinct()
            
        if (lowProbabilityDays.isNotEmpty() && lowProbabilityDays.size <= 3) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.TIME_CHANGE,
                    message = "根据预测，您在接下来的${lowProbabilityDays.joinToString("、") { it.name }}完成习惯的可能性较低，建议提前做好计划或调整这些天的安排。",
                    expectedImpact = "可能提高这些天的完成率15-25%",
                    confidence = 0.7f
                )
            )
        }
        
        // 6. Suggestions based on clustering
        if (clusters.size > 1) {
            // Find the cluster with highest completion rate
            val successCluster = clusters.maxByOrNull { cluster -> 
                cluster.value.count { it.isCompleted }.toFloat() / cluster.value.size 
            }
            
            if (successCluster != null && successCluster.value.size >= 5) {
                // Analyze what's common in the success cluster
                val timePattern = successCluster.value
                    .filter { it.completionTime != null }
                    .groupBy { it.completionTime!!.toLocalTime().hour / 6 } // Group by 6-hour blocks
                    .maxByOrNull { it.value.size }
                    ?.key
                    
                val timeDescription = when(timePattern) {
                    0 -> "凌晨(0-6点)"
                    1 -> "上午(6-12点)"
                    2 -> "下午(12-18点)"
                    3 -> "晚上(18-24点)"
                    else -> "一天中的特定时间"
                }
                
                if (timePattern != null) {
                    suggestions.add(
                        OptimizationSuggestion(
                            type = SuggestionType.HABIT_COMBINATION,
                            message = "分析显示，您在${timeDescription}完成习惯时最为成功。尝试将这个习惯固定在这个时间段，并与当时的其他活动结合。",
                            expectedImpact = "可能提高习惯的自动化程度和完成率",
                            confidence = 0.8f
                        )
                    )
                }
            }
        }
        
        // 7. Add a streak-based suggestion if applicable
        if (patterns.containsKey(PatternType.STREAK_BASED) && patterns[PatternType.STREAK_BASED]!! > 0.6) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.HABIT_COMBINATION,
                    message = "您的习惯表现出明显的连续性模式，建议使用'不要打破链条'的策略，可视化您的连续完成记录来增强动力。",
                    expectedImpact = "可能提高长期坚持率15-20%",
                    confidence = patterns[PatternType.STREAK_BASED]!!
                )
            )
        }
        
        // Ensure we have at least one suggestion
        if (suggestions.isEmpty()) {
            suggestions.add(
                OptimizationSuggestion(
                    type = SuggestionType.HABIT_COMBINATION,
                    message = "尝试将这个习惯与您已经养成的习惯结合起来，例如在刷牙后立即完成。",
                    expectedImpact = "可能提高习惯的自动化程度",
                    confidence = 0.65f
                )
            )
        }
        
        // Limit to top 3 suggestions with highest confidence
        return suggestions.sortedByDescending { it.confidence }.take(3)
    }
}