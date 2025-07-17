package com.andychen.habitgem.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.andychen.habitgem.data.database.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user preferences
 */
@Dao
interface UserPreferencesDao {
    /**
     * Get user preferences by user ID
     */
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getUserPreferences(userId: String): Flow<UserPreferencesEntity?>

    /**
     * Get user preferences by user ID (non-flow version)
     */
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getUserPreferencesSync(userId: String): UserPreferencesEntity?

    /**
     * Insert user preferences
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferencesEntity)

    /**
     * Update user preferences
     */
    @Update
    suspend fun updateUserPreferences(preferences: UserPreferencesEntity)

    /**
     * Delete user preferences
     */
    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun deleteUserPreferences(userId: String)

    /**
     * Get all user preferences that need to be synced
     */
    @Query("SELECT * FROM user_preferences WHERE lastSyncedAt < :timestamp")
    suspend fun getUnSyncedPreferences(timestamp: Long): List<UserPreferencesEntity>

    /**
     * Update last synced timestamp
     */
    @Query("UPDATE user_preferences SET lastSyncedAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastSyncedAt(userId: String, timestamp: Long)
}