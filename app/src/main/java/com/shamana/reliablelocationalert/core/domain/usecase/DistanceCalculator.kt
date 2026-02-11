package com.shamana.reliablelocationalert.core.domain.usecase

import android.location.Location
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.Destination

object DistanceCalculator {

    fun averageDistanceMeters(
        samples: List<LocationSample>,
        destination: Destination
    ): Float {
        val validSamples = samples.filter { it.accuracyMeters <= 50f }

        if (validSamples.isEmpty()) return Float.MAX_VALUE

        val distances = validSamples.map { sample ->
            val result = FloatArray(1)
            Location.distanceBetween(
                sample.latitude,
                sample.longitude,
                destination.latitude,
                destination.longitude,
                result
            )
            result[0]
        }

        return distances.average().toFloat()
    }
}
