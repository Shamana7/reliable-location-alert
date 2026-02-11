package com.shamana.reliablelocationalert.core.domain.model

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
    val speedMps: Float?
)
