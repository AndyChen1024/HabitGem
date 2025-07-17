package com.andychen.habitgem.data.repository

import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for habit-related operations
 */
interface HabitRepository {
    /**
     * Get all habits for a user
     */
    fun getHabitsByUserId(userId: String): Flow<List<Habit>>
    
    /**
     * Get a habit by ID
     */
    fun getHabitById(habitId: String): Flow<Habit?>
    
    /**
     * Get habits by category
     */
    fun getHabitsByCategory(userId: String, category: HabitCategory): Flow<List<Habit>>
    
    /**
     * Get AI-recommended habits
     */
    fun getAIRecommendedHabits(userId: String): Flow<List<Habit>>
    
    /**
     * Get active habits for a specific date
     */
    fun getActiveHabits(userId: String, date: LocalDate): Flow<List<Habit>>
    
    /**
     * Create a new habit
     */
    suspend fun createHabit(habit: Habit): String
    
    /**
     * Update an existing habit
     */
    suspend fun updateHabit(habit: Habit)
    
    /**
     * Delete a habit
     */
    suspend fun deleteHabit(habitId: String)
    
    /**
     * Get habit records for a habit
     */
    fun getHabitRecords(habitId: String): Flow<List<HabitRecord>>
    
    /**
     * Get a habit record for a specific date
     */
    fun getHabitRecordForDate(habitId: String, date: LocalDate): Flow<HabitRecord?>
    
    /**
     * Get habit records for a date range
     */
    fun getHabitRecordsByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitRecord>>
    
    /**
     * Create or update a habit record
     */
    suspend fun saveHabitRecord(record: HabitRecord)
    
    /**
     * Get the current streak for a habit
     */
    fun getCurrentStreak(habitId: String): Flow<Int>
    
    /**
     * Get the completion rate for a habit
     */
    fun getCompletionRate(habitId: String): Flow<Float>
    
    /**
     * Get the completion rate for a habit in a date range
     */
    fun getCompletionRateForDateRange(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<Float>
}