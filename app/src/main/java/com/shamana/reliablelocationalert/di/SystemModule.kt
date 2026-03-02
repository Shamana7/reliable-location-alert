package com.shamana.reliablelocationalert.di

import android.content.Context
import com.shamana.reliablelocationalert.core.system.alarm.AlarmManagerScheduler
import com.shamana.reliablelocationalert.core.system.location.FusedLocationProviderImpl
import com.shamana.reliablelocationalert.core.system.location.LocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

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
}
