package com.shamana.reliablelocationalert.core.domain.model

data class TrackingUiState(
    val isTracking: Boolean = false,
    val destination: Destination? = null,
    val state: TrackingState? = null,
    val lastLat: Double? = null,
    val lastLng: Double? = null
)
