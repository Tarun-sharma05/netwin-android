package com.cehpoint.netwin.di

import android.content.Context
import com.cehpoint.netwin.utils.NetworkStateMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideNetworkStateMonitor(
        @ApplicationContext context: Context
    ): NetworkStateMonitor {
        return NetworkStateMonitor(context)
    }
}