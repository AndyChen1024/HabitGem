package com.andychen.habitgem.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andychen.habitgem.domain.model.HabitRecommendation
import com.andychen.habitgem.ui.assistant.AIAssistantScreen
import com.andychen.habitgem.ui.preferences.UserPreferencesQuestionnaire
import com.andychen.habitgem.ui.recommendation.HabitRecommendationScreen

/**
 * Navigation routes for the app
 */
object AppRoutes {
    const val PREFERENCES_QUESTIONNAIRE = "preferences_questionnaire"
    const val HOME = "home"
    const val HABIT_RECOMMENDATION = "habit_recommendation"
    const val AI_ASSISTANT = "ai_assistant"
    // Add more routes as needed
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.PREFERENCES_QUESTIONNAIRE
) {
    val actions = remember(navController) { AppNavigationActions(navController) }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // User preferences questionnaire screen
        composable(AppRoutes.PREFERENCES_QUESTIONNAIRE) {
            UserPreferencesQuestionnaire(
                onComplete = { actions.navigateToHome() }
            )
        }
        
        // Home screen (placeholder for now)
        composable(AppRoutes.HOME) {
            // HomeScreen will be implemented in another task
            // For now, we'll just use a placeholder
            // HomeScreen(
            //     onAddHabit = { actions.navigateToHabitRecommendation() }
            // )
        }
        
        // Habit recommendation screen
        composable(AppRoutes.HABIT_RECOMMENDATION) {
            HabitRecommendationScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateManualHabit = { /* Will be implemented in another task */ },
                onAcceptRecommendation = { recommendation ->
                    // Handle accepting a recommendation
                    // For now, just navigate back to home
                    navController.popBackStack()
                }
            )
        }
        
        // AI Assistant screen
        composable(AppRoutes.AI_ASSISTANT) {
            AIAssistantScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Add more screens as needed
    }
}

/**
 * Navigation actions for the app
 */
class AppNavigationActions(private val navController: NavHostController) {
    
    /**
     * Navigate to home screen
     */
    fun navigateToHome() {
        navController.navigate(AppRoutes.HOME) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            popUpTo(AppRoutes.PREFERENCES_QUESTIONNAIRE) {
                inclusive = true
            }
        }
    }
    
    /**
     * Navigate to preferences questionnaire
     */
    fun navigateToPreferencesQuestionnaire() {
        navController.navigate(AppRoutes.PREFERENCES_QUESTIONNAIRE)
    }
    
    /**
     * Navigate to habit recommendation screen
     */
    fun navigateToHabitRecommendation() {
        navController.navigate(AppRoutes.HABIT_RECOMMENDATION)
    }
    
    /**
     * Navigate to AI assistant screen
     */
    fun navigateToAIAssistant() {
        navController.navigate(AppRoutes.AI_ASSISTANT)
    }
    
    // Add more navigation actions as needed
}