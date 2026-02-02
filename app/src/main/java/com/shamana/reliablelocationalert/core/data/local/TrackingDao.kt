package com.shamana.reliablelocationalert.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_session WHERE id = 1")
    suspend fun getSession(): TrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(entity: TrackingEntity)

    @Query("DELETE FROM tracking_session")
    suspend fun clear()
}
