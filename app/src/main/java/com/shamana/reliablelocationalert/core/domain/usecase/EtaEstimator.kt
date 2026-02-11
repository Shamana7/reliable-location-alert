package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import kotlin.math.max

object EtaEstimator {

    fun estimateSeconds(
        distanceMeters: Float,
        samples: List<LocationSample>
    ): Long {
        val speeds = samples.mapNotNull { it.speedMps }.filter { it > 0 }

        val avgSpeed = if (speeds.isNotEmpty()) {
            speeds.average().toFloat()
        } else {
            1.2f // fallback: walking speed
        }

        return (distanceMeters / max(avgSpeed, 0.5f)).toLong()      //time = distance/speed
    }
}
