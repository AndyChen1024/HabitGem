package com.andychen.habitgem.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.Mood
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: LocalDateTime,
    val lastActive: LocalDateTime
)

@Entity(
    tableName = "habits",
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
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val category: HabitCategory,
    val frequency: Frequency,
    val reminderTime: String?,
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

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("userId"), Index("date")]
)
data class HabitRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val userId: String,
    val date: LocalDate,
    val isCompleted: Boolean,
    val completionTime: LocalDateTime?,
    val note: String?,
    val mood: Mood?,
    val difficulty: Int? // User feedback on difficulty 1-5
)

@Entity(
    tableName = "ai_feedbacks",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("habitId")]
)
data class AIFeedbackEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val habitId: String?,
    val feedbackType: String,
    val message: String,
    val generatedAt: LocalDateTime,
    val isRead: Boolean,
    val metadata: Map<String, String>?
)