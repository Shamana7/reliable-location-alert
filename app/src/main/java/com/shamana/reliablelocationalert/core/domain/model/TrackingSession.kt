package com.shamana.reliablelocationalert.core.domain.model

data class TrackingSession(
    val destination: Destination,
    val state: TrackingState,
    val lastKnownLatitude: Double?,
    val lastKnownLongitude: Double?,
    val lastUpdatedAt: Long,
    val distanceMeters: Float? = null,
    val etaSeconds: Long? = null,
    val progress: Float? = null
)
