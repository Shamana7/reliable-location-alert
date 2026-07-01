package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.LocationSample

object EtaEstimator {

    private const val FALLBACK_WALKING_SPEED_MPS = 1.2f
    private const val MIN_SPEED_MPS = 0.5f
    private const val MAX_SPEED_MPS = 50.0f

    fun estimateSeconds(
        distanceMeters: Float,
        samples: List<LocationSample>
    ): Long {

        val speeds = samples
            .mapNotNull { it.speedMps }
            .filter { it > 0 }

        val avgSpeed = if (speeds.isNotEmpty()) {
            speeds.average().toFloat()
        } else {
            FALLBACK_WALKING_SPEED_MPS
        }

        val safeSpeed = avgSpeed.coerceIn(
            MIN_SPEED_MPS,
            MAX_SPEED_MPS
        )

        return (distanceMeters / safeSpeed).toLong()
    }
}