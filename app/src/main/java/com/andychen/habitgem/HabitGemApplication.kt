package com.andychen.habitgem

import android.app.Application
import com.andychen.habitgem.di.aiModule
import com.andychen.habitgem.di.apiModule
import com.andychen.habitgem.di.databaseModule
import com.andychen.habitgem.di.repositoryModule
import com.andychen.habitgem.di.viewModelModule
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

@HiltAndroidApp
class HabitGemApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // We're keeping Koin initialization for now to avoid breaking existing code
        // Later we can migrate all dependencies to Hilt
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HabitGemApplication)
            modules(
                listOf(
                    databaseModule,
                    apiModule,
                    repositoryModule,
                    aiModule,
                    viewModelModule
                )
            )
        }
    }
}