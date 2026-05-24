package com.recordly.app.di

import android.content.Context

import com.recordly.app.data.PreferencesRepository
import com.recordly.app.data.MediaRepository

/**
 * Dependency Injection container interface.
 */
interface AppContainer {
    val preferencesRepository: PreferencesRepository
    val mediaRepository: MediaRepository
}

/**
 * Implementation of AppContainer for manual dependency injection.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    
    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context)
    }
    
    override val mediaRepository: MediaRepository by lazy {
        MediaRepository(context)
    }
}
