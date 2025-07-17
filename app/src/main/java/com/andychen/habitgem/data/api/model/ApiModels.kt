package com.andychen.habitgem.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Habit Recommendation API models
 */
@Serializable
data class HabitRecommendationRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("preferences") val preferences: UserPreferencesDto? = null,
    @SerialName("existing_habits") val existingHabits: List<String>? = null
)

@Serializable
data class HabitRecommendationResponse(
    @SerialName("recommendations") val recommendations: List<HabitRecommendationDto>,
    @SerialName("request_id") val requestId: String
)

@Serializable
data class HabitRecommendationDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("category") val category: String,
    @SerialName("difficulty") val difficulty: Int,
    @SerialName("recommendation_reason") val recommendationReason: String,
    @SerialName("scientific_basis") val scientificBasis: String,
    @SerialName("suggested_frequency") val suggestedFrequency: FrequencyDto,
    @SerialName("estimated_time_per_day") val estimatedTimePerDay: Int
)

/**
 * Progress Feedback API models
 */
@Serializable
data class ProgressFeedbackRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String? = null,
    @SerialName("feedback_type") val feedbackType: String,
    @SerialName("context_data") val contextData: Map<String, String>? = null
)

@Serializable
data class ProgressFeedbackResponse(
    @SerialName("feedback") val feedback: FeedbackMessageDto,
    @SerialName("request_id") val requestId: String
)

@Serializable
data class FeedbackMessageDto(
    @SerialName("message") val message: String,
    @SerialName("type") val type: String,
    @SerialName("emoji") val emoji: String? = null,
    @SerialName("animation_type") val animationType: String? = null
)

/**
 * Habit Analysis API models
 */
@Serializable
data class HabitAnalysisRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String? = null,
    @SerialName("analysis_type") val analysisType: String,
    @SerialName("time_range") val timeRange: TimeRangeDto? = null
)

@Serializable
data class HabitAnalysisResponse(
    @SerialName("insights") val insights: List<HabitInsightDto>,
    @SerialName("suggestions") val suggestions: List<OptimizationSuggestionDto>? = null,
    @SerialName("request_id") val requestId: String
)

@Serializable
data class HabitInsightDto(
    @SerialName("habit_id") val habitId: String,
    @SerialName("best_performing_days") val bestPerformingDays: List<String>,
    @SerialName("completion_trend") val completionTrend: String,
    @SerialName("consistency_score") val consistencyScore: Float,
    @SerialName("insight_message") val insightMessage: String
)

@Serializable
data class OptimizationSuggestionDto(
    @SerialName("type") val type: String,
    @SerialName("message") val message: String,
    @SerialName("expected_impact") val expectedImpact: String,
    @SerialName("confidence") val confidence: Float
)

/**
 * AI Assistant API models
 */
@Serializable
data class AIAssistantRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("message") val message: String,
    @SerialName("context") val context: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null
)

@Serializable
data class AIAssistantResponse(
    @SerialName("message") val message: String,
    @SerialName("type") val type: String,
    @SerialName("related_habits") val relatedHabits: List<String>? = null,
    @SerialName("action_suggestions") val actionSuggestions: List<AssistantActionDto>? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("request_id") val requestId: String
)

@Serializable
data class AssistantActionDto(
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("payload") val payload: Map<String, String>? = null
)

/**
 * User Preferences API models
 */
@Serializable
data class UserPreferencesResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("preferences") val preferences: UserPreferencesDto,
    @SerialName("sync_timestamp") val syncTimestamp: Long,
    @SerialName("request_id") val requestId: String
)

/**
 * Common data models
 */
@Serializable
data class UserPreferencesDto(
    @SerialName("habit_categories") val habitCategories: List<String>,
    @SerialName("goal_types") val goalTypes: List<String>,
    @SerialName("difficulty_preference") val difficultyPreference: Int,
    @SerialName("time_availability") val timeAvailability: Map<String, List<TimeSlotDto>>? = null
)

@Serializable
data class FrequencyDto(
    @SerialName("type") val type: String,
    @SerialName("times_per_week") val timesPerWeek: Int? = null,
    @SerialName("days_of_week") val daysOfWeek: List<String>? = null
)

@Serializable
data class TimeSlotDto(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

@Serializable
data class TimeRangeDto(
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String
)