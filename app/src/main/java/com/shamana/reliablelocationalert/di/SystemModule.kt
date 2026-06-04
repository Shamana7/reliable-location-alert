package com.shamana.reliablelocationalert.di

import android.content.Context
import com.shamana.reliablelocationalert.core.system.alarm.AlarmManagerScheduler
import com.shamana.reliablelocationalert.core.system.location.FusedLocationProviderImpl
import com.shamana.reliablelocationalert.core.system.location.LocationProvider
import com.shamana.reliablelocationalert.core.system.permission.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    fun provideLocationProvider(
        @ApplicationContext context: Context
    ): LocationProvider {
        return FusedLocationProviderImpl(context)
    }

    @Provides
    fun provideAlarmScheduler(
        @ApplicationContext context: Context
    ): AlarmManagerScheduler {
        return AlarmManagerScheduler(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager {
        return PermissionManager(context)
    }
}