package com.shamana.reliablelocationalert.di

import com.shamana.reliablelocationalert.core.data.local.TrackingDao
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepository
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTrackingRepository(
        dao: TrackingDao
    ): TrackingRepository {
        return TrackingRepositoryImpl(dao)
    }
}
