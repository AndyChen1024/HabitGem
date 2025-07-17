package com.andychen.habitgem.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andychen.habitgem.data.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId")
    fun getHabitsByUserId(userId: String): Flow<List<HabitEntity>>
    
    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitById(habitId: String): Flow<HabitEntity?>
    
    @Query("SELECT * FROM habits WHERE userId = :userId AND category = :category")
    fun getHabitsByCategory(userId: String, category: String): Flow<List<HabitEntity>>
    
    @Query("SELECT * FROM habits WHERE userId = :userId AND isAIRecommended = 1")
    fun getAIRecommendedHabits(userId: String): Flow<List<HabitEntity>>
    
    @Query("SELECT * FROM habits WHERE userId = :userId AND startDate <= :date AND (targetDays IS NULL OR startDate + targetDays >= :date)")
    fun getActiveHabits(userId: String, date: LocalDate): Flow<List<HabitEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)
    
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)
}