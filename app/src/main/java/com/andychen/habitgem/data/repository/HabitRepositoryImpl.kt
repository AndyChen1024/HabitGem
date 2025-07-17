package com.andychen.habitgem.data.repository

import com.andychen.habitgem.data.database.dao.HabitDao
import com.andychen.habitgem.data.database.dao.HabitRecordDao
import com.andychen.habitgem.data.database.entity.HabitEntity
import com.andychen.habitgem.data.database.entity.HabitRecordEntity
import com.andychen.habitgem.domain.model.Habit
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.HabitRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Implementation of HabitRepository
 */
class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitRecordDao: HabitRecordDao
) : HabitRepository {
    
    override fun getHabitsByUserId(userId: String): Flow<List<Habit>> {
        return habitDao.getHabitsByUserId(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getHabitById(habitId: String): Flow<Habit?> {
        return habitDao.getHabitById(habitId).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override fun getHabitsByCategory(userId: String, category: HabitCategory): Flow<List<Habit>> {
        return habitDao.getHabitsByCategory(userId, category.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getAIRecommendedHabits(userId: String): Flow<List<Habit>> {
        return habitDao.getAIRecommendedHabits(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getActiveHabits(userId: String, date: LocalDate): Flow<List<Habit>> {
        return habitDao.getActiveHabits(userId, date).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun createHabit(habit: Habit): String {
        val habitId = habit.id.ifEmpty { UUID.randomUUID().toString() }
        val habitEntity = habit.copy(id = habitId).toEntity()
        habitDao.insertHabit(habitEntity)
        return habitId
    }
    
    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.toEntity())
    }
    
    override suspend fun deleteHabit(habitId: String) {
        habitDao.deleteHabit(habitId)
    }
    
    override fun getHabitRecords(habitId: String): Flow<List<HabitRecord>> {
        return habitRecordDao.getRecordsByHabitId(habitId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getHabitRecordForDate(habitId: String, date: LocalDate): Flow<HabitRecord?> {
        return habitRecordDao.getRecordByHabitIdAndDate(habitId, date).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override fun getHabitRecordsByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitRecord>> {
        return habitRecordDao.getRecordsByDateRange(userId, startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun saveHabitRecord(record: HabitRecord) {
        val recordId = record.id.ifEmpty { UUID.randomUUID().toString() }
        val recordEntity = record.copy(id = recordId).toEntity()
        habitRecordDao.insertRecord(recordEntity)
    }
    
    override fun getCurrentStreak(habitId: String): Flow<Int> {
        return habitRecordDao.getCurrentStreakByDate(habitId, LocalDate.now())
    }
    
    override fun getCompletionRate(habitId: String): Flow<Float> {
        // This is a simplified implementation
        // In a real app, we would calculate this based on the habit's frequency
        return habitRecordDao.getCompletedCountByHabitId(habitId).map { completedCount ->
            // For now, we'll just return the raw count as a placeholder
            completedCount.toFloat()
        }
    }
    
    override fun getCompletionRateForDateRange(
        habitId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Float> {
        return habitRecordDao.getCompletedCountByDateRange(habitId, startDate, endDate).map { completedCount ->
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
            completedCount.toFloat() / totalDays
        }
    }
    
    /**
     * Extension function to convert HabitEntity to domain model
     */
    private fun HabitEntity.toDomainModel(): Habit {
        return Habit(
            id = this.id,
            userId = this.userId,
            name = this.name,
            description = this.description,
            category = this.category,
            frequency = this.frequency,
            reminderSettings = null, // We would parse the reminderTime string to create a ReminderSettings object
            startDate = this.startDate,
            targetDays = this.targetDays,
            color = this.color,
            icon = this.icon,
            isAIRecommended = this.isAIRecommended,
            difficulty = this.difficulty,
            scientificBasis = this.scientificBasis,
            createdAt = this.createdAt,
            lastModified = this.lastModified
        )
    }
    
    /**
     * Extension function to convert Habit to entity
     */
    private fun Habit.toEntity(): HabitEntity {
        return HabitEntity(
            id = this.id,
            userId = this.userId,
            name = this.name,
            description = this.description,
            category = this.category,
            frequency = this.frequency,
            reminderTime = this.reminderSettings?.time?.toString(),
            startDate = this.startDate,
            targetDays = this.targetDays,
            color = this.color,
            icon = this.icon,
            isAIRecommended = this.isAIRecommended,
            difficulty = this.difficulty,
            scientificBasis = this.scientificBasis,
            createdAt = this.createdAt,
            lastModified = this.lastModified
        )
    }
    
    /**
     * Extension function to convert HabitRecordEntity to domain model
     */
    private fun HabitRecordEntity.toDomainModel(): HabitRecord {
        return HabitRecord(
            id = this.id,
            habitId = this.habitId,
            userId = this.userId,
            date = this.date,
            isCompleted = this.isCompleted,
            completionTime = this.completionTime,
            note = this.note,
            mood = this.mood,
            difficulty = this.difficulty
        )
    }
    
    /**
     * Extension function to convert HabitRecord to entity
     */
    private fun HabitRecord.toEntity(): HabitRecordEntity {
        return HabitRecordEntity(
            id = this.id,
            habitId = this.habitId,
            userId = this.userId,
            date = this.date,
            isCompleted = this.isCompleted,
            completionTime = this.completionTime,
            note = this.note,
            mood = this.mood,
            difficulty = this.difficulty
        )
    }
}