package com.andychen.habitgem.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ReminderPreferences
import com.andychen.habitgem.domain.model.TimeSlot
import com.andychen.habitgem.domain.model.UserPreferences
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Entity for storing user preferences in the database
 */
@Entity(
    tableName = "user_preferences",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class UserPreferencesEntity(
    @PrimaryKey val userId: String,
    val habitCategories: List<HabitCategory>,
    val goalTypes: List<GoalType>,
    val reminderEnabled: Boolean,
    val reminderDefaultTime: String?,
    val reminderSound: String?,
    val reminderVibrationEnabled: Boolean,
    val difficultyPreference: Int,
    val timeAvailability: Map<DayOfWeek, List<TimeSlot>>,
    val lastSyncedAt: Long
) {
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): UserPreferences {
        return UserPreferences(
            habitCategories = habitCategories,
            goalTypes = goalTypes,
            reminderPreferences = ReminderPreferences(
                enabled = reminderEnabled,
                defaultTime = reminderDefaultTime?.let { LocalTime.parse(it) },
                notificationSound = reminderSound,
                vibrationEnabled = reminderVibrationEnabled
            ),
            difficultyPreference = difficultyPreference,
            timeAvailability = timeAvailability
        )
    }

    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(userId: String, preferences: UserPreferences, lastSyncedAt: Long = System.currentTimeMillis()): UserPreferencesEntity {
            return UserPreferencesEntity(
                userId = userId,
                habitCategories = preferences.habitCategories,
                goalTypes = preferences.goalTypes,
                reminderEnabled = preferences.reminderPreferences.enabled,
                reminderDefaultTime = preferences.reminderPreferences.defaultTime?.toString(),
                reminderSound = preferences.reminderPreferences.notificationSound,
                reminderVibrationEnabled = preferences.reminderPreferences.vibrationEnabled,
                difficultyPreference = preferences.difficultyPreference,
                timeAvailability = preferences.timeAvailability,
                lastSyncedAt = lastSyncedAt
            )
        }
    }
}