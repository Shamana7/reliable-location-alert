package com.shamana.reliablelocationalert.core.domain.model

data class Destination(
    val latitude: Double,
    val longitude: Double,
    val alertRadiusMeters: Float
)
