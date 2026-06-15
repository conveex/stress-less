package com.stressless.app.di

import android.content.Context
import com.stressless.app.data.local.SessionPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSessionPreferences(
        @ApplicationContext context: Context
    ): SessionPreferences {
        return SessionPreferences(context)
    }
}