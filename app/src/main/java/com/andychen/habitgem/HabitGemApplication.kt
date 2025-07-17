package com.andychen.habitgem

import android.app.Application
import com.andychen.habitgem.di.aiModule
import com.andychen.habitgem.di.apiModule
import com.andychen.habitgem.di.databaseModule
import com.andychen.habitgem.di.repositoryModule
import com.andychen.habitgem.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class HabitGemApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
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