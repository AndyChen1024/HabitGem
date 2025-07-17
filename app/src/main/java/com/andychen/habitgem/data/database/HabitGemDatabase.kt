package com.andychen.habitgem.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andychen.habitgem.data.database.dao.HabitDao
import com.andychen.habitgem.data.database.dao.HabitRecordDao
import com.andychen.habitgem.data.database.dao.UserDao
import com.andychen.habitgem.data.database.dao.UserPreferencesDao
import com.andychen.habitgem.data.database.entity.HabitEntity
import com.andychen.habitgem.data.database.entity.HabitRecordEntity
import com.andychen.habitgem.data.database.entity.UserEntity
import com.andychen.habitgem.data.database.entity.UserPreferencesEntity

@Database(
    entities = [
        UserEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitGemDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}