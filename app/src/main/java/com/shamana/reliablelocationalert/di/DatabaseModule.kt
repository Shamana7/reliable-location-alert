package com.shamana.reliablelocationalert.di

import android.content.Context
import androidx.room.Room
import com.shamana.reliablelocationalert.core.data.local.AppDatabase
import com.shamana.reliablelocationalert.core.data.local.TrackingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rla_database"
        ).build()
    }

    @Provides
    fun provideTrackingDao(db: AppDatabase): TrackingDao {
        return db.trackingDao()
    }
}
