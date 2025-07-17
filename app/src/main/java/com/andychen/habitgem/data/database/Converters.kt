package com.andychen.habitgem.data.database

import androidx.room.TypeConverter
import com.andychen.habitgem.domain.model.Frequency
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.Mood
import com.andychen.habitgem.domain.model.TimeSlot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Type converters for Room database
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(java.time.ZoneOffset.UTC)
    }
    
    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }
    
    @TypeConverter
    fun fromLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }
    
    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? {
        return time?.toString()
    }
    
    @TypeConverter
    fun fromHabitCategory(value: HabitCategory): String {
        return value.name
    }
    
    @TypeConverter
    fun toHabitCategory(value: String): HabitCategory {
        return HabitCategory.valueOf(value)
    }
    
    @TypeConverter
    fun fromHabitCategoryList(categories: List<HabitCategory>): String {
        return json.encodeToString(categories.map { it.name })
    }
    
    @TypeConverter
    fun toHabitCategoryList(value: String): List<HabitCategory> {
        return json.decodeFromString<List<String>>(value).map { HabitCategory.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGoalTypeList(goalTypes: List<GoalType>): String {
        return json.encodeToString(goalTypes.map { it.name })
    }
    
    @TypeConverter
    fun toGoalTypeList(value: String): List<GoalType> {
        return json.decodeFromString<List<String>>(value).map { GoalType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromFrequency(frequency: Frequency): String {
        return json.encodeToString(frequency)
    }
    
    @TypeConverter
    fun toFrequency(value: String): Frequency {
        return json.decodeFromString(value)
    }
    
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toDayOfWeekList(value: String?): List<DayOfWeek>? {
        return value?.split(",")?.map { DayOfWeek.valueOf(it) }
    }
    
    @TypeConverter
    fun fromMood(mood: Mood?): String? {
        return mood?.name
    }
    
    @TypeConverter
    fun toMood(value: String?): Mood? {
        return value?.let { Mood.valueOf(it) }
    }
    
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return map?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let { json.decodeFromString(it) }
    }
    
    @TypeConverter
    fun fromTimeSlotMap(map: Map<DayOfWeek, List<TimeSlot>>): String {
        // Convert to a serializable format
        val serializedMap = map.mapKeys { it.key.name }
            .mapValues { entry ->
                entry.value.map { slot ->
                    mapOf(
                        "startTime" to slot.startTime.toString(),
                        "endTime" to slot.endTime.toString()
                    )
                }
            }
        return json.encodeToString(serializedMap)
    }
    
    @TypeConverter
    fun toTimeSlotMap(value: String): Map<DayOfWeek, List<TimeSlot>> {
        val serializedMap: Map<String, List<Map<String, String>>> = json.decodeFromString(value)
        return serializedMap.mapKeys { DayOfWeek.valueOf(it.key) }
            .mapValues { entry ->
                entry.value.map { slotMap ->
                    TimeSlot(
                        startTime = LocalTime.parse(slotMap["startTime"]),
                        endTime = LocalTime.parse(slotMap["endTime"])
                    )
                }
            }
    }
}