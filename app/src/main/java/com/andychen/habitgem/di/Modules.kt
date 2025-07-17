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
import com.andychen.habitgem.ui.preferences.UserPreferencesViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            HabitGemDatabase::class.java,
            "habitgem_database"
        ).build()
    }
    
    single { get<HabitGemDatabase>().habitDao() }
    single { get<HabitGemDatabase>().habitRecordDao() }
    single { get<HabitGemDatabase>().userDao() }
    single { get<HabitGemDatabase>().userPreferencesDao() }
    single { androidContext().dataStore }
}

val apiModule = module {
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    single {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        Retrofit.Builder()
            .baseUrl(ApiClient.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    single { get<Retrofit>().create(AIServiceApi::class.java) }
    
    // Create ApiClient instance
    single { ApiClient(get()) }
}

val repositoryModule = module {
    single<HabitRepository> { HabitRepositoryImpl(get(), get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get(), get(), get(), get()) }
}

val aiModule = module {
    single<HabitRecommendationService> { HabitRecommendationServiceImpl(get(), get()) }
    single<ProgressFeedbackService> { ProgressFeedbackServiceImpl(get(), get()) }
    single<HabitAnalysisService> { HabitAnalysisServiceImpl(get(), get()) }
    single<AIAssistantService> { AIAssistantServiceImpl(get(), get()) }
}

val viewModelModule = module {
    // ViewModels will be added here
    viewModel { UserPreferencesViewModel(get()) }
}