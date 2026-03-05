package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import kotlin.math.max
import kotlin.math.min

object EtaEstimator {

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
            1.2f   // fallback walking speed
        }

        // clamp speed to realistic range
        val safeSpeed = min(max(avgSpeed, 0.5f), 3.0f)

        return (distanceMeters / safeSpeed).toLong()
    }
}
