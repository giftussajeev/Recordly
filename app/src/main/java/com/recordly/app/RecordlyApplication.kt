package com.recordly.app

import android.app.Application
import com.recordly.app.di.AppContainer
import com.recordly.app.di.DefaultAppContainer

class RecordlyApplication : Application() {
    
    // Manual DI Container
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
