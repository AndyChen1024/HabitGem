package com.andychen.habitgem.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.andychen.habitgem.data.api.AIServiceApi
import com.andychen.habitgem.data.api.ApiClient
import com.andychen.habitgem.data.database.HabitGemDatabase
import com.andychen.habitgem.data.repository.HabitRepository
import com.andychen.habitgem.data.repository.HabitRepositoryImpl
import com.andychen.habitgem.data.repository.UserPreferencesRepository
import com.andychen.habitgem.data.repository.UserPreferencesRepositoryImpl
import com.andychen.habitgem.domain.ai.AIAssistantService
import com.andychen.habitgem.domain.ai.AIAssistantServiceImpl
import com.andychen.habitgem.domain.ai.HabitAnalysisService
import com.andychen.habitgem.domain.ai.HabitAnalysisServiceImpl
import com.andychen.habitgem.domain.ai.HabitRecommendationService
import com.andychen.habitgem.domain.ai.HabitRecommendationServiceImpl
import com.andychen.habitgem.domain.ai.ProgressFeedbackService
import com.andychen.habitgem.domain.ai.ProgressFeedbackServiceImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitGemDatabase {
        return Room.databaseBuilder(
            context,
            HabitGemDatabase::class.java,
            "habitgem_database"
        ).build()
    }
    
    @Provides
    fun provideHabitDao(database: HabitGemDatabase) = database.habitDao()
    
    @Provides
    fun provideHabitRecordDao(database: HabitGemDatabase) = database.habitRecordDao()
    
    @Provides
    fun provideUserDao(database: HabitGemDatabase) = database.userDao()
    
    @Provides
    fun provideUserPreferencesDao(database: HabitGemDatabase) = database.userPreferencesDao()
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        return Retrofit.Builder()
            .baseUrl(ApiClient.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAIServiceApi(retrofit: Retrofit): AIServiceApi {
        return retrofit.create(AIServiceApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideApiClient(aiServiceApi: AIServiceApi): ApiClient {
        return ApiClient(aiServiceApi)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: com.andychen.habitgem.data.database.dao.HabitDao,
        habitRecordDao: com.andychen.habitgem.data.database.dao.HabitRecordDao
    ): HabitRepository {
        return HabitRepositoryImpl(habitDao, habitRecordDao)
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>,
        userDao: com.andychen.habitgem.data.database.dao.UserDao,
        userPreferencesDao: com.andychen.habitgem.data.database.dao.UserPreferencesDao,
        apiClient: ApiClient
    ): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(dataStore, userDao, userPreferencesDao, apiClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    
    @Provides
    @Singleton
    fun provideHabitRecommendationService(
        aiServiceApi: AIServiceApi,
        habitRepository: HabitRepository
    ): HabitRecommendationService {
        return HabitRecommendationServiceImpl(aiServiceApi, habitRepository)
    }
    
    @Provides
    @Singleton
    fun provideProgressFeedbackService(
        aiServiceApi: AIServiceApi,
        habitRepository: HabitRepository
    ): ProgressFeedbackService {
        return ProgressFeedbackServiceImpl(aiServiceApi, habitRepository)
    }
    
    @Provides
    @Singleton
    fun provideHabitAnalysisService(
        aiServiceApi: AIServiceApi,
        habitRepository: HabitRepository
    ): HabitAnalysisService {
        return HabitAnalysisServiceImpl(aiServiceApi, habitRepository)
    }
    
    @Provides
    @Singleton
    fun provideAIAssistantService(
        aiServiceApi: AIServiceApi,
        habitRepository: HabitRepository,
        userPreferencesRepository: UserPreferencesRepository
    ): AIAssistantService {
        return AIAssistantServiceImpl(aiServiceApi, habitRepository, userPreferencesRepository)
    }
}