package com.andychen.habitgem.data.api

import com.andychen.habitgem.data.api.model.AIAssistantRequest
import com.andychen.habitgem.data.api.model.AIAssistantResponse
import com.andychen.habitgem.data.api.model.HabitAnalysisRequest
import com.andychen.habitgem.data.api.model.HabitAnalysisResponse
import com.andychen.habitgem.data.api.model.HabitRecommendationRequest
import com.andychen.habitgem.data.api.model.HabitRecommendationResponse
import com.andychen.habitgem.data.api.model.ProgressFeedbackRequest
import com.andychen.habitgem.data.api.model.ProgressFeedbackResponse
import com.andychen.habitgem.data.api.model.UserPreferencesDto
import com.andychen.habitgem.data.api.model.UserPreferencesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * API client for AI services
 */
class ApiClient(private val aiServiceApi: AIServiceApi) {
    // Base URL for API
    companion object {
        const val BASE_URL = "https://api.habitgem.com/"
    }
    
    /**
     * Get habit recommendations from AI service
     */
    suspend fun getHabitRecommendations(request: HabitRecommendationRequest): HabitRecommendationResponse {
        return aiServiceApi.getHabitRecommendations(request)
    }
    
    /**
     * Get progress feedback from AI service
     */
    suspend fun getProgressFeedback(request: ProgressFeedbackRequest): ProgressFeedbackResponse {
        return aiServiceApi.getProgressFeedback(request)
    }
    
    /**
     * Get habit analysis from AI service
     */
    suspend fun getHabitAnalysis(request: HabitAnalysisRequest): HabitAnalysisResponse {
        return aiServiceApi.getHabitAnalysis(request)
    }
    
    /**
     * Get assistant response from AI service
     */
    suspend fun getAssistantResponse(request: AIAssistantRequest): AIAssistantResponse {
        return aiServiceApi.getAssistantResponse(request)
    }
    
    /**
     * Update user preferences on server
     */
    suspend fun updateUserPreferences(userId: String, preferences: UserPreferencesDto): Response<UserPreferencesResponse> {
        return aiServiceApi.updateUserPreferences(userId, preferences)
    }
}

/**
 * Interface for AI service API endpoints
 */
interface AIServiceApi {
    @POST("ai/recommendations")
    suspend fun getHabitRecommendations(
        @Body request: HabitRecommendationRequest
    ): HabitRecommendationResponse
    
    @POST("ai/feedback")
    suspend fun getProgressFeedback(
        @Body request: ProgressFeedbackRequest
    ): ProgressFeedbackResponse
    
    @POST("ai/analysis")
    suspend fun getHabitAnalysis(
        @Body request: HabitAnalysisRequest
    ): HabitAnalysisResponse
    
    @POST("ai/assistant")
    suspend fun getAssistantResponse(
        @Body request: AIAssistantRequest
    ): AIAssistantResponse
    
    @PUT("users/{userId}/preferences")
    suspend fun updateUserPreferences(
        @Path("userId") userId: String,
        @Body preferences: UserPreferencesDto
    ): Response<UserPreferencesResponse>
}