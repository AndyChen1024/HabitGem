package com.andychen.habitgem.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * User models
 */
data class User(
    val id: String,
    val name: String,
    val preferences: UserPreferences,
    val createdAt: LocalDateTime,
    val lastActive: LocalDateTime
)

data class UserPreferences(
    val habitCategories: List<HabitCategory>,
    val goalTypes: List<GoalType>,
    val reminderPreferences: ReminderPreferences,
    val difficultyPreference: Int, // 1-5
    val timeAvailability: Map<DayOfWeek, List<TimeSlot>>
)

data class ReminderPreferences(
    val enabled: Boolean,
    val defaultTime: LocalTime?,
    val notificationSound: String?,
    val vibrationEnabled: Boolean
)

data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime
)

/**
 * Habit models
 */
data class Habit(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val category: HabitCategory,
    val frequency: Frequency,
    val reminderSettings: ReminderSettings?,
    val startDate: LocalDate,
    val targetDays: Int?,
    val color: String,
    val icon: String,
    val isAIRecommended: Boolean,
    val difficulty: Int, // 1-5
    val scientificBasis: String?,
    val createdAt: LocalDateTime,
    val lastModified: LocalDateTime
)

data class ReminderSettings(
    val time: LocalTime,
    val daysOfWeek: List<DayOfWeek>?,
    val enabled: Boolean
)

data class HabitRecord(
    val id: String,
    val habitId: String,
    val userId: String,
    val date: LocalDate,
    val isCompleted: Boolean,
    val completionTime: LocalDateTime?,
    val note: String?,
    val mood: Mood?,
    val difficulty: Int? // User feedback on difficulty 1-5
)

/**
 * AI models
 */
data class HabitRecommendation(
    val id: String,
    val name: String,
    val description: String,
    val category: HabitCategory,
    val difficulty: Int, // 1-5
    val recommendationReason: String,
    val scientificBasis: String,
    val suggestedFrequency: Frequency,
    val estimatedTimePerDay: Int // minutes
)

data class HabitEvidence(
    val habitId: String,
    val scientificBasis: String,
    val references: List<String>,
    val benefitsSummary: String
)

data class FeedbackMessage(
    val message: String,
    val type: FeedbackType,
    val emoji: String?,
    val animationType: AnimationType?
)

data class ProgressAnalysis(
    val completionRate: Float,
    val streak: Int,
    val insight: String,
    val suggestion: String,
    val visualData: List<DataPoint>
)

data class PeriodicReport(
    val period: ReportPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completionRate: Float,
    val habitsSummary: Map<String, Float>, // habitId to completion rate
    val insights: List<String>,
    val recommendations: List<String>
)

data class HabitInsight(
    val habitId: String,
    val bestPerformingDays: List<DayOfWeek>,
    val completionTrend: Trend,
    val consistencyScore: Float,
    val insightMessage: String
)

data class OptimizationSuggestion(
    val type: SuggestionType,
    val message: String,
    val expectedImpact: String,
    val confidence: Float // 0.0-1.0
)

data class HabitCorrelation(
    val habitId1: String,
    val habitId2: String,
    val correlationType: CorrelationType,
    val correlationStrength: Float, // -1.0 to 1.0
    val description: String
)

data class AssistantResponse(
    val message: String,
    val type: ResponseType,
    val relatedHabits: List<String>?,
    val actionSuggestions: List<AssistantAction>?
)

data class AssistantAction(
    val type: ActionType,
    val title: String,
    val payload: Map<String, String>?
)

data class DataPoint(
    val date: LocalDate,
    val value: Float,
    val label: String?
)

/**
 * Enums
 */
enum class HabitCategory {
    HEALTH,
    FITNESS,
    MINDFULNESS,
    PRODUCTIVITY,
    LEARNING,
    SOCIAL,
    CREATIVITY,
    FINANCE,
    OTHER
}

enum class GoalType {
    HEALTH_IMPROVEMENT,
    SKILL_DEVELOPMENT,
    PRODUCTIVITY_BOOST,
    STRESS_REDUCTION,
    RELATIONSHIP_BUILDING,
    PERSONAL_GROWTH,
    OTHER
}

enum class Mood {
    VERY_HAPPY,
    HAPPY,
    NEUTRAL,
    UNHAPPY,
    VERY_UNHAPPY
}

enum class FeedbackType {
    COMPLETION,
    STREAK,
    MILESTONE,
    MISSED,
    GENERAL
}

enum class AnimationType {
    CONFETTI,
    FIREWORKS,
    SPARKLE,
    THUMBS_UP,
    NONE
}

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class Trend {
    IMPROVING,
    STABLE,
    DECLINING,
    FLUCTUATING,
    NOT_ENOUGH_DATA
}

enum class SuggestionType {
    TIME_CHANGE,
    FREQUENCY_ADJUST,
    DIFFICULTY_ADJUST,
    HABIT_COMBINATION,
    HABIT_REPLACEMENT
}

enum class CorrelationType {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

enum class ResponseType {
    TEXT,
    SUGGESTION,
    ANALYSIS
}

enum class ActionType {
    VIEW_HABIT,
    CREATE_HABIT,
    MODIFY_HABIT,
    VIEW_ANALYSIS,
    EXTERNAL_LINK
}

/**
 * Frequency model
 */
sealed class Frequency {
    data class Daily(val timesPerDay: Int = 1) : Frequency()
    data class Weekly(val daysOfWeek: List<DayOfWeek>) : Frequency()
    data class Monthly(val daysOfMonth: List<Int>) : Frequency()
    data class Interval(val everyNDays: Int) : Frequency()
}