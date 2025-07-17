package com.andychen.habitgem.domain.ai.analysis

import com.andychen.habitgem.domain.model.HabitRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Analyzer for habit patterns and time series data
 * Implements pattern recognition algorithms for habit data
 */
class HabitPatternAnalyzer {

    /**
     * Analyzes time series data to identify patterns in habit completion
     * @param records List of habit records to analyze
     * @return Map of pattern types to their confidence scores (0.0-1.0)
     */
    fun identifyTimeSeriesPatterns(records: List<HabitRecord>): Map<PatternType, Float> {
        if (records.size < 7) {
            return mapOf(PatternType.NOT_ENOUGH_DATA to 1.0f)
        }

        val patterns = mutableMapOf<PatternType, Float>()
        
        // Sort records by date
        val sortedRecords = records.sortedBy { it.date }
        
        // Check for weekly patterns
        val weekdayPattern = detectWeekdayPattern(sortedRecords)
        if (weekdayPattern.second > 0.6f) {
            patterns[PatternType.WEEKDAY_PATTERN] = weekdayPattern.second
            patterns[weekdayPattern.first] = weekdayPattern.second
        }
        
        // Check for streak patterns
        val streakPattern = detectStreakPattern(sortedRecords)
        if (streakPattern > 0.6f) {
            patterns[PatternType.STREAK_BASED] = streakPattern
        }
        
        // Check for time of day patterns (if completion time is available)
        val timePattern = detectTimeOfDayPattern(sortedRecords)
        if (timePattern.second > 0.6f) {
            patterns[PatternType.TIME_OF_DAY] = timePattern.second
            patterns[timePattern.first] = timePattern.second
        }
        
        // Check for declining trend
        val trendPattern = detectTrendPattern(sortedRecords)
        patterns[trendPattern] = 0.7f
        
        return patterns
    }
    
    /**
     * Performs clustering on habit records to identify groups of similar behavior
     * @param records List of habit records to cluster
     * @param clusterCount Number of clusters to identify
     * @return Map of cluster IDs to lists of records in that cluster
     */
    fun clusterHabitRecords(records: List<HabitRecord>, clusterCount: Int = 3): Map<Int, List<HabitRecord>> {
        if (records.size < clusterCount * 2) {
            return mapOf(0 to records)
        }
        
        // Extract features for clustering
        val features = records.map { record ->
            HabitFeatures(
                dayOfWeek = record.date.dayOfWeek.value.toFloat() / 7f, // Normalize to 0-1
                isCompleted = if (record.isCompleted) 1f else 0f,
                timeOfDay = record.completionTime?.toLocalTime()?.toSecondOfDay()?.toFloat()?.div(86400f) ?: 0.5f, // Normalize to 0-1
                difficulty = record.difficulty?.toFloat()?.div(5f) ?: 0.5f // Normalize to 0-1
            )
        }
        
        // Perform k-means clustering
        val clusters = kMeansClustering(features, clusterCount)
        
        // Map clusters back to records
        return clusters.mapValues { (clusterId, indices) ->
            indices.map { records[it] }
        }
    }
    
    /**
     * Detects anomalies in habit records that deviate from normal patterns
     * @param records List of habit records to analyze
     * @param sensitivityThreshold Threshold for anomaly detection (lower = more sensitive)
     * @return List of anomalous records with their anomaly scores
     */
    fun detectAnomalies(records: List<HabitRecord>, sensitivityThreshold: Float = 2.0f): List<Pair<HabitRecord, Float>> {
        if (records.size < 14) {
            return emptyList()
        }
        
        // Sort records by date
        val sortedRecords = records.sortedBy { it.date }
        
        // Calculate completion rate baseline
        val completionRate = sortedRecords.count { it.isCompleted }.toFloat() / sortedRecords.size
        
        // Calculate day of week baselines
        val dayOfWeekBaselines = sortedRecords
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, dayRecords) -> 
                dayRecords.count { it.isCompleted }.toFloat() / dayRecords.size
            }
            
        // Calculate moving average of completion (window size = 7 days)
        val movingAverages = calculateMovingAverages(sortedRecords, 7)
        
        // Detect anomalies
        val anomalies = mutableListOf<Pair<HabitRecord, Float>>()
        
        for (i in sortedRecords.indices) {
            val record = sortedRecords[i]
            val dayBaseline = dayOfWeekBaselines[record.date.dayOfWeek] ?: completionRate
            val movingAvg = movingAverages.getOrNull(i) ?: completionRate
            
            // Calculate expected completion probability
            val expected = (dayBaseline + movingAvg) / 2
            
            // Calculate anomaly score
            val actual = if (record.isCompleted) 1f else 0f
            val anomalyScore = abs(actual - expected) / (expected * (1 - expected) + 0.1f)
            
            // If anomaly score exceeds threshold, add to anomalies
            if (anomalyScore > sensitivityThreshold) {
                anomalies.add(record to anomalyScore)
            }
        }
        
        return anomalies
    }
    
    /**
     * Predicts future habit completion probability
     * @param records List of past habit records
     * @param targetDate Date to predict completion for
     * @return Probability of habit completion (0.0-1.0)
     */
    fun predictCompletionProbability(records: List<HabitRecord>, targetDate: LocalDate): Float {
        if (records.isEmpty()) {
            return 0.5f // Default probability with no data
        }
        
        // Sort records by date
        val sortedRecords = records.sortedBy { it.date }
        
        // Calculate overall completion rate
        val overallRate = sortedRecords.count { it.isCompleted }.toFloat() / sortedRecords.size
        
        // Calculate day of week specific rate
        val dayOfWeek = targetDate.dayOfWeek
        val daySpecificRecords = sortedRecords.filter { it.date.dayOfWeek == dayOfWeek }
        val daySpecificRate = if (daySpecificRecords.isNotEmpty()) {
            daySpecificRecords.count { it.isCompleted }.toFloat() / daySpecificRecords.size
        } else {
            overallRate
        }
        
        // Calculate recent trend (last 14 days or less)
        val recentRecords = sortedRecords.takeLast(14)
        val recentRate = if (recentRecords.isNotEmpty()) {
            recentRecords.count { it.isCompleted }.toFloat() / recentRecords.size
        } else {
            overallRate
        }
        
        // Calculate streak factor
        val currentStreak = calculateCurrentStreak(sortedRecords)
        val streakFactor = when {
            currentStreak >= 7 -> 0.2f
            currentStreak >= 3 -> 0.1f
            currentStreak <= -3 -> -0.1f
            currentStreak <= -7 -> -0.2f
            else -> 0f
        }
        
        // Combine factors with weights
        val probability = (daySpecificRate * 0.4f) + (recentRate * 0.4f) + (overallRate * 0.2f) + streakFactor
        
        // Clamp to valid probability range
        return probability.coerceIn(0.01f, 0.99f)
    }
    
    /**
     * Detects weekly patterns in habit completion
     * @return Pair of specific pattern type and confidence score
     */
    private fun detectWeekdayPattern(records: List<HabitRecord>): Pair<PatternType, Float> {
        // Count completions by day of week
        val dayCompletions = records
            .filter { it.isCompleted }
            .groupBy { it.date.dayOfWeek }
            .mapValues { it.value.size }
            
        // Count total records by day of week
        val dayTotals = records
            .groupBy { it.date.dayOfWeek }
            .mapValues { it.value.size }
            
        // Calculate completion rates by day
        val dayRates = dayTotals.keys.associateWith { day ->
            val completions = dayCompletions[day] ?: 0
            val total = dayTotals[day] ?: 0
            if (total > 0) completions.toFloat() / total else 0f
        }
        
        // Check for weekday vs weekend pattern
        val weekdayRate = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                               DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            .mapNotNull { dayRates[it] }
            .average().toFloat()
            
        val weekendRate = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            .mapNotNull { dayRates[it] }
            .average().toFloat()
        
        val weekdayWeekendDiff = abs(weekdayRate - weekendRate)
        
        // Check for specific day patterns
        val avgRate = dayRates.values.average().toFloat()
        val dayVariances = dayRates.mapValues { abs(it.value - avgRate) }
        
        val maxVarianceDay = dayVariances.maxByOrNull { it.value }?.key
        val maxVariance = dayVariances.maxOfOrNull { it.value } ?: 0f
        
        // Determine pattern type and confidence
        return when {
            weekdayWeekendDiff > 0.3f && weekdayRate > weekendRate -> 
                PatternType.WEEKDAY_PREFERENCE to (weekdayWeekendDiff * 2).coerceAtMost(1f)
            weekdayWeekendDiff > 0.3f && weekendRate > weekdayRate -> 
                PatternType.WEEKEND_PREFERENCE to (weekdayWeekendDiff * 2).coerceAtMost(1f)
            maxVariance > 0.3f && maxVarianceDay != null -> 
                PatternType.SPECIFIC_DAY_PATTERN to (maxVariance * 2).coerceAtMost(1f)
            else -> 
                PatternType.NO_WEEKLY_PATTERN to 0f
        }
    }
    
    /**
     * Detects streak-based patterns in habit completion
     * @return Confidence score for streak pattern
     */
    private fun detectStreakPattern(records: List<HabitRecord>): Float {
        if (records.size < 14) return 0f
        
        // Sort records by date
        val sortedRecords = records.sortedBy { it.date }
        
        // Find streaks (consecutive completed or missed days)
        val streaks = mutableListOf<Int>()
        var currentStreak = 0
        var lastCompleted = false
        
        for (record in sortedRecords) {
            if (record.isCompleted == lastCompleted) {
                currentStreak++
            } else {
                if (currentStreak > 0) {
                    streaks.add(if (lastCompleted) currentStreak else -currentStreak)
                }
                currentStreak = 1
                lastCompleted = record.isCompleted
            }
        }
        
        // Add the final streak
        if (currentStreak > 0) {
            streaks.add(if (lastCompleted) currentStreak else -currentStreak)
        }
        
        // Calculate average streak length
        val avgStreakLength = streaks.map { abs(it) }.average()
        
        // Calculate streak pattern confidence
        val streakConfidence = when {
            avgStreakLength > 5 -> 0.9f
            avgStreakLength > 3 -> 0.7f
            avgStreakLength > 2 -> 0.5f
            else -> 0.3f
        }
        
        return streakConfidence
    }
    
    /**
     * Detects time of day patterns in habit completion
     * @return Pair of time pattern type and confidence score
     */
    private fun detectTimeOfDayPattern(records: List<HabitRecord>): Pair<PatternType, Float> {
        // Filter records with completion time
        val recordsWithTime = records.filter { 
            it.isCompleted && it.completionTime != null 
        }
        
        if (recordsWithTime.size < 7) {
            return PatternType.NO_TIME_PATTERN to 0f
        }
        
        // Extract hours of day
        val hours = recordsWithTime.map { 
            it.completionTime!!.toLocalTime().hour 
        }
        
        // Count completions by time period
        val morning = hours.count { it in 5..11 }
        val afternoon = hours.count { it in 12..17 }
        val evening = hours.count { it in 18..22 }
        val night = hours.count { it in 23..23 || it in 0..4 }
        
        val total = morning + afternoon + evening + night
        
        // Calculate percentages
        val morningPct = morning.toFloat() / total
        val afternoonPct = afternoon.toFloat() / total
        val eveningPct = evening.toFloat() / total
        val nightPct = night.toFloat() / total
        
        // Find dominant time period
        val maxPct = maxOf(morningPct, afternoonPct, eveningPct, nightPct)
        val dominance = maxPct - (morningPct + afternoonPct + eveningPct + nightPct - maxPct) / 3
        
        // Determine pattern type and confidence
        val (patternType, confidence) = when {
            maxPct < 0.4f -> PatternType.NO_TIME_PATTERN to 0f
            morningPct == maxPct -> PatternType.MORNING_PREFERENCE to dominance
            afternoonPct == maxPct -> PatternType.AFTERNOON_PREFERENCE to dominance
            eveningPct == maxPct -> PatternType.EVENING_PREFERENCE to dominance
            else -> PatternType.NIGHT_PREFERENCE to dominance
        }
        
        return patternType to confidence
    }
    
    /**
     * Detects trend patterns in habit completion
     * @return Trend pattern type
     */
    private fun detectTrendPattern(records: List<HabitRecord>): PatternType {
        if (records.size < 14) return PatternType.NOT_ENOUGH_DATA
        
        // Sort records by date
        val sortedRecords = records.sortedBy { it.date }
        
        // Split into two halves
        val midpoint = sortedRecords.size / 2
        val firstHalf = sortedRecords.take(midpoint)
        val secondHalf = sortedRecords.takeLast(midpoint)
        
        // Calculate completion rates for each half
        val firstHalfRate = firstHalf.count { it.isCompleted }.toFloat() / firstHalf.size
        val secondHalfRate = secondHalf.count { it.isCompleted }.toFloat() / secondHalf.size
        
        // Determine trend
        return when {
            secondHalfRate > firstHalfRate * 1.2f -> PatternType.IMPROVING_TREND
            secondHalfRate < firstHalfRate * 0.8f -> PatternType.DECLINING_TREND
            abs(secondHalfRate - firstHalfRate) < 0.1f -> PatternType.STABLE_TREND
            else -> PatternType.FLUCTUATING_TREND
        }
    }
    
    /**
     * Calculates moving averages for completion rates
     */
    private fun calculateMovingAverages(records: List<HabitRecord>, windowSize: Int): List<Float> {
        if (records.size < windowSize) {
            return emptyList()
        }
        
        val result = mutableListOf<Float>()
        
        for (i in windowSize - 1 until records.size) {
            val windowRecords = records.subList(i - windowSize + 1, i + 1)
            val windowAvg = windowRecords.count { it.isCompleted }.toFloat() / windowSize
            result.add(windowAvg)
        }
        
        return result
    }
    
    /**
     * Calculates the current streak (positive for completion streak, negative for missing streak)
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
    
    /**
     * K-means clustering implementation for habit features
     */
    private fun kMeansClustering(
        features: List<HabitFeatures>, 
        k: Int, 
        maxIterations: Int = 100
    ): Map<Int, List<Int>> {
        if (features.isEmpty() || k <= 0 || k > features.size) {
            return emptyMap()
        }
        
        // Initialize centroids randomly
        val centroids = (0 until k).map { 
            features[it % features.size].copy() 
        }.toMutableList()
        
        var iterations = 0
        var changed = true
        
        // Cluster assignments
        val assignments = MutableList(features.size) { 0 }
        
        while (changed && iterations < maxIterations) {
            changed = false
            
            // Assign points to nearest centroid
            for (i in features.indices) {
                val feature = features[i]
                val distances = centroids.map { centroid -> 
                    euclideanDistance(feature, centroid) 
                }
                val nearestCentroid = distances.withIndex().minByOrNull { it.value }?.index ?: 0
                
                if (assignments[i] != nearestCentroid) {
                    assignments[i] = nearestCentroid
                    changed = true
                }
            }
            
            // Update centroids
            for (j in 0 until k) {
                val clusterPoints = features.filterIndexed { idx, _ -> assignments[idx] == j }
                
                if (clusterPoints.isNotEmpty()) {
                    centroids[j] = HabitFeatures(
                        dayOfWeek = clusterPoints.map { it.dayOfWeek }.average().toFloat(),
                        isCompleted = clusterPoints.map { it.isCompleted }.average().toFloat(),
                        timeOfDay = clusterPoints.map { it.timeOfDay }.average().toFloat(),
                        difficulty = clusterPoints.map { it.difficulty }.average().toFloat()
                    )
                }
            }
            
            iterations++
        }
        
        // Group indices by cluster
        return assignments.withIndex()
            .groupBy({ it.value }, { it.index })
    }
    
    /**
     * Calculate Euclidean distance between two feature vectors
     */
    private fun euclideanDistance(a: HabitFeatures, b: HabitFeatures): Double {
        return sqrt(
            (a.dayOfWeek - b.dayOfWeek).pow(2) +
            (a.isCompleted - b.isCompleted).pow(2) +
            (a.timeOfDay - b.timeOfDay).pow(2) +
            (a.difficulty - b.difficulty).pow(2)
        )
    }
    
    /**
     * Extension function for squaring a float
     */
    private fun Float.pow(exponent: Int): Float {
        var result = 1f
        repeat(exponent) { result *= this }
        return result
    }
}

/**
 * Feature vector for habit clustering
 */
data class HabitFeatures(
    val dayOfWeek: Float, // 0.0-1.0 (normalized day of week)
    val isCompleted: Float, // 0.0 or 1.0
    val timeOfDay: Float, // 0.0-1.0 (normalized time of day)
    val difficulty: Float // 0.0-1.0 (normalized difficulty)
)

/**
 * Types of patterns that can be detected in habit data
 */
enum class PatternType {
    // Weekly patterns
    WEEKDAY_PREFERENCE,
    WEEKEND_PREFERENCE,
    SPECIFIC_DAY_PATTERN,
    NO_WEEKLY_PATTERN,
    
    // Time of day patterns
    MORNING_PREFERENCE,
    AFTERNOON_PREFERENCE,
    EVENING_PREFERENCE,
    NIGHT_PREFERENCE,
    NO_TIME_PATTERN,
    
    // Trend patterns
    IMPROVING_TREND,
    DECLINING_TREND,
    STABLE_TREND,
    FLUCTUATING_TREND,
    
    // Other patterns
    STREAK_BASED,
    WEEKDAY_PATTERN,
    TIME_OF_DAY,
    NOT_ENOUGH_DATA
}