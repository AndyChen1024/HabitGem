package com.andychen.habitgem.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andychen.habitgem.data.api.ApiClient
import com.andychen.habitgem.data.api.model.UserPreferencesDto
import com.andychen.habitgem.data.database.dao.UserDao
import com.andychen.habitgem.data.database.dao.UserPreferencesDao
import com.andychen.habitgem.data.database.entity.UserPreferencesEntity
import com.andychen.habitgem.domain.model.GoalType
import com.andychen.habitgem.domain.model.HabitCategory
import com.andychen.habitgem.domain.model.ReminderPreferences
import com.andychen.habitgem.domain.model.TimeSlot
import com.andychen.habitgem.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Implementation of UserPreferencesRepository using both DataStore and Room database
 * with remote synchronization capabilities
 */
class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val userDao: UserDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val apiClient: ApiClient
) : UserPreferencesRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Preference keys for DataStore
    private val keyPrefix = "user_preferences_"
    private fun getCategoriesKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_categories")
    private fun getGoalTypesKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_goal_types")
    private fun getReminderEnabledKey(userId: String) = booleanPreferencesKey("${keyPrefix}${userId}_reminder_enabled")
    private fun getReminderTimeKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_reminder_time")
    private fun getReminderSoundKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_reminder_sound")
    private fun getReminderVibrationKey(userId: String) = booleanPreferencesKey("${keyPrefix}${userId}_reminder_vibration")
    private fun getDifficultyKey(userId: String) = intPreferencesKey("${keyPrefix}${userId}_difficulty")
    private fun getTimeAvailabilityKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_time_availability")
    private fun getLastSyncKey(userId: String) = stringPreferencesKey("${keyPrefix}${userId}_last_sync")
    
    /**
     * Get user preferences from both local database and DataStore
     * Prioritizes database storage but falls back to DataStore if needed
     */
    override fun getUserPreferences(userId: String): Flow<UserPreferences?> {
        // First try to get from Room database
        return userPreferencesDao.getUserPreferences(userId)
            .map { entity -> entity?.toDomainModel() }
            .catch { e ->
                // If database fails, fall back to DataStore
                emit(getPreferencesFromDataStore(userId).firstOrNull())
            }
    }
    
    /**
     * Save user preferences to both local storage and sync with remote server
     */
    override suspend fun saveUserPreferences(userId: String, preferences: UserPreferences) {
        withContext(Dispatchers.IO) {
            try {
                // Save to Room database
                val entity = UserPreferencesEntity.fromDomainModel(userId, preferences)
                userPreferencesDao.insertUserPreferences(entity)
                
                // Save to DataStore as backup
                saveToDataStore(userId, preferences)
                
                // Sync with remote server
                syncWithRemoteServer(userId, preferences)
            } catch (e: Exception) {
                // If database fails, at least save to DataStore
                saveToDataStore(userId, preferences)
            }
        }
    }
    
    /**
     * Update preferred habit categories
     */
    override suspend fun updatePreferredCategories(userId: String, categories: List<HabitCategory>) {
        withContext(Dispatchers.IO) {
            try {
                // Get current preferences
                val currentPrefs = getUserPreferences(userId).firstOrNull() ?: getDefaultPreferences()
                
                // Update categories
                val updatedPrefs = currentPrefs.copy(habitCategories = categories)
                
                // Save updated preferences
                saveUserPreferences(userId, updatedPrefs)
            } catch (e: Exception) {
                // Fallback to DataStore only
                dataStore.edit { prefs ->
                    prefs[getCategoriesKey(userId)] = json.encodeToString(categories.map { it.name })
                }
            }
        }
    }
    
    /**
     * Update preferred goal types
     */
    override suspend fun updatePreferredGoalTypes(userId: String, goalTypes: List<GoalType>) {
        withContext(Dispatchers.IO) {
            try {
                // Get current preferences
                val currentPrefs = getUserPreferences(userId).firstOrNull() ?: getDefaultPreferences()
                
                // Update goal types
                val updatedPrefs = currentPrefs.copy(goalTypes = goalTypes)
                
                // Save updated preferences
                saveUserPreferences(userId, updatedPrefs)
            } catch (e: Exception) {
                // Fallback to DataStore only
                dataStore.edit { prefs ->
                    prefs[getGoalTypesKey(userId)] = json.encodeToString(goalTypes.map { it.name })
                }
            }
        }
    }
    
    /**
     * Update reminder preferences
     */
    override suspend fun updateReminderPreferences(userId: String, reminderPreferences: ReminderPreferences) {
        withContext(Dispatchers.IO) {
            try {
                // Get current preferences
                val currentPrefs = getUserPreferences(userId).firstOrNull() ?: getDefaultPreferences()
                
                // Update reminder preferences
                val updatedPrefs = currentPrefs.copy(reminderPreferences = reminderPreferences)
                
                // Save updated preferences
                saveUserPreferences(userId, updatedPrefs)
            } catch (e: Exception) {
                // Fallback to DataStore only
                dataStore.edit { prefs ->
                    prefs[getReminderEnabledKey(userId)] = reminderPreferences.enabled
                    reminderPreferences.defaultTime?.let {
                        prefs[getReminderTimeKey(userId)] = it.toString()
                    }
                    reminderPreferences.notificationSound?.let {
                        prefs[getReminderSoundKey(userId)] = it
                    }
                    prefs[getReminderVibrationKey(userId)] = reminderPreferences.vibrationEnabled
                }
            }
        }
    }
    
    /**
     * Update difficulty preference
     */
    override suspend fun updateDifficultyPreference(userId: String, difficultyPreference: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Get current preferences
                val currentPrefs = getUserPreferences(userId).firstOrNull() ?: getDefaultPreferences()
                
                // Update difficulty preference
                val updatedPrefs = currentPrefs.copy(difficultyPreference = difficultyPreference)
                
                // Save updated preferences
                saveUserPreferences(userId, updatedPrefs)
            } catch (e: Exception) {
                // Fallback to DataStore only
                dataStore.edit { prefs ->
                    prefs[getDifficultyKey(userId)] = difficultyPreference
                }
            }
        }
    }
    
    /**
     * Update time availability
     */
    override suspend fun updateTimeAvailability(userId: String, timeAvailability: Map<DayOfWeek, List<TimeSlot>>) {
        withContext(Dispatchers.IO) {
            try {
                // Get current preferences
                val currentPrefs = getUserPreferences(userId).firstOrNull() ?: getDefaultPreferences()
                
                // Update time availability
                val updatedPrefs = currentPrefs.copy(timeAvailability = timeAvailability)
                
                // Save updated preferences
                saveUserPreferences(userId, updatedPrefs)
            } catch (e: Exception) {
                // Fallback to DataStore only
                dataStore.edit { prefs ->
                    val timeAvailabilityJson = json.encodeToString(
                        timeAvailability.mapKeys { it.key.name }
                            .mapValues { entry -> entry.value.map { TimeSlotDto.fromDomainModel(it) } }
                    )
                    prefs[getTimeAvailabilityKey(userId)] = timeAvailabilityJson
                }
            }
        }
    }
    
    /**
     * Sync user preferences with remote server
     */
    override suspend fun syncUserPreferences(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Get local preferences
                val localPrefs = getUserPreferences(userId).firstOrNull() ?: return@withContext false
                
                // Convert to DTO for API
                val prefsDto = convertToDto(localPrefs)
                
                // Send to server
                val response = apiClient.updateUserPreferences(userId, prefsDto)
                
                if (response.isSuccessful) {
                    // Update last synced timestamp
                    val currentTime = System.currentTimeMillis()
                    userPreferencesDao.updateLastSyncedAt(userId, currentTime)
                    
                    // Also update in DataStore
                    dataStore.edit { prefs ->
                        prefs[getLastSyncKey(userId)] = currentTime.toString()
                    }
                    
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Sync all unsynced preferences with remote server
     */
    override suspend fun syncAllPendingPreferences(): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Get timestamp from 24 hours ago
                val syncThreshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
                
                // Get all preferences that haven't been synced in the last 24 hours
                val unsyncedPrefs = userPreferencesDao.getUnSyncedPreferences(syncThreshold)
                
                var syncedCount = 0
                
                // Sync each one
                unsyncedPrefs.forEach { entity ->
                    val userId = entity.userId
                    val prefs = entity.toDomainModel()
                    
                    try {
                        // Convert to DTO for API
                        val prefsDto = convertToDto(prefs)
                        
                        // Send to server
                        val response = apiClient.updateUserPreferences(userId, prefsDto)
                        
                        if (response.isSuccessful) {
                            // Update last synced timestamp
                            val currentTime = System.currentTimeMillis()
                            userPreferencesDao.updateLastSyncedAt(userId, currentTime)
                            syncedCount++
                        }
                    } catch (e: Exception) {
                        // Continue with next preference if one fails
                    }
                }
                
                syncedCount
            } catch (e: Exception) {
                0
            }
        }
    }
    
    /**
     * Get preferences from DataStore
     */
    private fun getPreferencesFromDataStore(userId: String): Flow<UserPreferences?> {
        return dataStore.data.map { preferences ->
            try {
                // Read all preferences
                val categoriesJson = preferences[getCategoriesKey(userId)] ?: return@map null
                val goalTypesJson = preferences[getGoalTypesKey(userId)] ?: return@map null
                val reminderEnabled = preferences[getReminderEnabledKey(userId)] ?: false
                val reminderTimeStr = preferences[getReminderTimeKey(userId)]
                val reminderSound = preferences[getReminderSoundKey(userId)]
                val reminderVibration = preferences[getReminderVibrationKey(userId)] ?: true
                val difficulty = preferences[getDifficultyKey(userId)] ?: 3
                val timeAvailabilityJson = preferences[getTimeAvailabilityKey(userId)]
                
                // Parse JSON data
                val categories = json.decodeFromString<List<String>>(categoriesJson)
                    .map { HabitCategory.valueOf(it) }
                val goalTypes = json.decodeFromString<List<String>>(goalTypesJson)
                    .map { GoalType.valueOf(it) }
                val reminderTime = reminderTimeStr?.let { LocalTime.parse(it) }
                val timeAvailability = timeAvailabilityJson?.let {
                    json.decodeFromString<Map<String, List<TimeSlotDto>>>(it)
                        .mapKeys { entry -> DayOfWeek.valueOf(entry.key) }
                        .mapValues { entry -> entry.value.map { dto -> dto.toDomainModel() } }
                } ?: emptyMap()
                
                // Create UserPreferences object
                UserPreferences(
                    habitCategories = categories,
                    goalTypes = goalTypes,
                    reminderPreferences = ReminderPreferences(
                        enabled = reminderEnabled,
                        defaultTime = reminderTime,
                        notificationSound = reminderSound,
                        vibrationEnabled = reminderVibration
                    ),
                    difficultyPreference = difficulty,
                    timeAvailability = timeAvailability
                )
            } catch (e: Exception) {
                // Return default preferences if there's an error
                getDefaultPreferences()
            }
        }
    }
    
    /**
     * Save preferences to DataStore
     */
    private suspend fun saveToDataStore(userId: String, preferences: UserPreferences) {
        dataStore.edit { prefs ->
            // Convert lists to JSON
            val categoriesJson = json.encodeToString(preferences.habitCategories.map { it.name })
            val goalTypesJson = json.encodeToString(preferences.goalTypes.map { it.name })
            val timeAvailabilityJson = json.encodeToString(
                preferences.timeAvailability.mapKeys { it.key.name }
                    .mapValues { entry -> entry.value.map { TimeSlotDto.fromDomainModel(it) } }
            )
            
            // Save all preferences
            prefs[getCategoriesKey(userId)] = categoriesJson
            prefs[getGoalTypesKey(userId)] = goalTypesJson
            prefs[getReminderEnabledKey(userId)] = preferences.reminderPreferences.enabled
            preferences.reminderPreferences.defaultTime?.let {
                prefs[getReminderTimeKey(userId)] = it.toString()
            }
            preferences.reminderPreferences.notificationSound?.let {
                prefs[getReminderSoundKey(userId)] = it
            }
            prefs[getReminderVibrationKey(userId)] = preferences.reminderPreferences.vibrationEnabled
            prefs[getDifficultyKey(userId)] = preferences.difficultyPreference
            prefs[getTimeAvailabilityKey(userId)] = timeAvailabilityJson
        }
    }
    
    /**
     * Sync preferences with remote server
     */
    private suspend fun syncWithRemoteServer(userId: String, preferences: UserPreferences) {
        try {
            // Convert to DTO for API
            val prefsDto = convertToDto(preferences)
            
            // Send to server
            val response = apiClient.updateUserPreferences(userId, prefsDto)
            
            if (response.isSuccessful) {
                // Update last synced timestamp
                val currentTime = System.currentTimeMillis()
                userPreferencesDao.updateLastSyncedAt(userId, currentTime)
                
                // Also update in DataStore
                dataStore.edit { prefs ->
                    prefs[getLastSyncKey(userId)] = currentTime.toString()
                }
            }
        } catch (e: Exception) {
            // Sync failed, will be retried later
        }
    }
    
    /**
     * Convert domain model to DTO for API
     */
    private fun convertToDto(preferences: UserPreferences): UserPreferencesDto {
        return UserPreferencesDto(
            habitCategories = preferences.habitCategories.map { it.name },
            goalTypes = preferences.goalTypes.map { it.name },
            difficultyPreference = preferences.difficultyPreference,
            timeAvailability = preferences.timeAvailability.mapKeys { it.key.name }
                .mapValues { entry ->
                    entry.value.map {
                        com.andychen.habitgem.data.api.model.TimeSlotDto(
                            startTime = it.startTime.toString(),
                            endTime = it.endTime.toString()
                        )
                    }
                }
        )
    }
    
    /**
     * Get default preferences for new users
     */
    private fun getDefaultPreferences(): UserPreferences {
        return UserPreferences(
            habitCategories = listOf(
                HabitCategory.HEALTH,
                HabitCategory.MINDFULNESS,
                HabitCategory.PRODUCTIVITY
            ),
            goalTypes = listOf(
                GoalType.HEALTH_IMPROVEMENT,
                GoalType.STRESS_REDUCTION,
                GoalType.PRODUCTIVITY_BOOST
            ),
            reminderPreferences = ReminderPreferences(
                enabled = true,
                defaultTime = LocalTime.of(8, 0),
                notificationSound = "default",
                vibrationEnabled = true
            ),
            difficultyPreference = 3,
            timeAvailability = mapOf(
                DayOfWeek.MONDAY to listOf(
                    TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                    TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.TUESDAY to listOf(
                    TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                    TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.WEDNESDAY to listOf(
                    TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                    TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.THURSDAY to listOf(
                    TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                    TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.FRIDAY to listOf(
                    TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)),
                    TimeSlot(LocalTime.of(19, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.SATURDAY to listOf(
                    TimeSlot(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    TimeSlot(LocalTime.of(15, 0), LocalTime.of(22, 0))
                ),
                DayOfWeek.SUNDAY to listOf(
                    TimeSlot(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    TimeSlot(LocalTime.of(15, 0), LocalTime.of(22, 0))
                )
            )
        )
    }
    
    /**
     * Data class for serializing TimeSlot in DataStore
     */
    private data class TimeSlotDto(
        val startTime: String,
        val endTime: String
    ) {
        fun toDomainModel(): TimeSlot {
            return TimeSlot(
                startTime = LocalTime.parse(startTime),
                endTime = LocalTime.parse(endTime)
            )
        }
        
        companion object {
            fun fromDomainModel(timeSlot: TimeSlot): TimeSlotDto {
                return TimeSlotDto(
                    startTime = timeSlot.startTime.toString(),
                    endTime = timeSlot.endTime.toString()
                )
            }
        }
    }
}