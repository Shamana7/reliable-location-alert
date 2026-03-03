package com.shamana.reliablelocationalert.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracking_session")
data class TrackingEntity(
    @PrimaryKey val id: Int = 1,
    val destinationLat: Double,
    val destinationLng: Double,
    val radius: Float,
    val state: String,
    val lastLat: Double?,
    val lastLng: Double?,
    val updatedAt: Long,
    val distanceMeters: Float?,
    val etaSeconds: Long?
)
