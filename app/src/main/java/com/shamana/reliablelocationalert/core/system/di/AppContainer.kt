package com.shamana.reliablelocationalert.core.system.di

import android.content.Context
import androidx.room.Room
import com.shamana.reliablelocationalert.core.data.local.AppDatabase
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepository
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepositoryImpl

class AppContainer(context: Context) {

    private val database: AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rla_database"
        ).build()

    val trackingRepository: TrackingRepository =
        TrackingRepositoryImpl(database.trackingDao())
}
