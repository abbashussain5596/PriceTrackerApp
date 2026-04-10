package com.abbas.pricetrackerapp

import android.app.Application
import com.abbas.pricetrackerapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PriceTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PriceTrackerApplication)
            modules(appModule)
        }
    }
}
