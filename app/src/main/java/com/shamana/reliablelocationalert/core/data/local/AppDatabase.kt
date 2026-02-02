package com.shamana.reliablelocationalert.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TrackingEntity::class],
    version = 1
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun trackingDao(): TrackingDao
}