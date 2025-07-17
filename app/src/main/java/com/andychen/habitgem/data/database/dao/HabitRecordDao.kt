package com.andychen.habitgem.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andychen.habitgem.data.database.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitRecordDao {
    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getRecordsByHabitId(habitId: String): Flow<List<HabitRecordEntity>>
    
    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date")
    fun getRecordByHabitIdAndDate(habitId: String, date: LocalDate): Flow<HabitRecordEntity?>
    
    @Query("SELECT * FROM habit_records WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsByDateRange(userId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitRecordEntity>>
    
    @Query("SELECT COUNT(*) FROM habit_records WHERE habitId = :habitId AND isCompleted = 1")
    fun getCompletedCountByHabitId(habitId: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM habit_records WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate AND isCompleted = 1")
    fun getCompletedCountByDateRange(habitId: String, startDate: LocalDate, endDate: LocalDate): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HabitRecordEntity)
    
    @Update
    suspend fun updateRecord(record: HabitRecordEntity)
    
    @Query("DELETE FROM habit_records WHERE habitId = :habitId")
    suspend fun deleteRecordsByHabitId(habitId: String)
    
    @Query("""
        SELECT COUNT(*) FROM habit_records 
        WHERE habitId = :habitId 
        AND isCompleted = 1 
        AND date < :date 
        AND date >= (
            SELECT MAX(date) FROM habit_records 
            WHERE habitId = :habitId 
            AND isCompleted = 0 
            AND date < :date
        )
    """)
    fun getCurrentStreakByDate(habitId: String, date: LocalDate): Flow<Int>
}