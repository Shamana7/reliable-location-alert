package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.TrackingState

class ProcessLocationUpdateUseCase(
    private val destination: Destination
) {

    private val buffer = LocationSampleBuffer()

    private var nearDestinationCount = 0

    private var poorAccuracyCount = 0
    private var goodAccuracyCount = 0

    private companion object {

        const val DEGRADED_ACCURACY_METERS = 500f

        const val DEGRADE_SAMPLE_THRESHOLD = 3
        const val RECOVERY_SAMPLE_THRESHOLD = 3
    }

    fun process(
        sample: LocationSample,
        currentState: TrackingState
    ): TrackingState {

        buffer.add(sample)

        if (sample.accuracyMeters > DEGRADED_ACCURACY_METERS) {
            poorAccuracyCount++
            goodAccuracyCount = 0
        } else {
            goodAccuracyCount++
            poorAccuracyCount = 0
        }

        if (
            currentState == TrackingState.TRACKING_ACTIVE &&
            poorAccuracyCount >= DEGRADE_SAMPLE_THRESHOLD
        ) {
            return TrackingState.TRACKING_DEGRADED
        }

        if (
            currentState == TrackingState.TRACKING_DEGRADED &&
            goodAccuracyCount >= RECOVERY_SAMPLE_THRESHOLD
        ) {
            return TrackingState.TRACKING_ACTIVE
        }

        if (!buffer.isFull()) {
            return currentState
        }

        val samples = buffer.samples()

        val avgDistance =
            DistanceCalculator.averageDistanceMeters(samples, destination)

        val etaSeconds =
            if (avgDistance == Float.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                EtaEstimator.estimateSeconds(
                    avgDistance,
                    samples
                )
            }

        val isNear =
            avgDistance <= destination.alertRadiusMeters ||
                    etaSeconds <= 60 // 1 minute ETA

        if (isNear) {
            nearDestinationCount++
        } else {
            nearDestinationCount = 0
        }

        return if (
            currentState == TrackingState.TRACKING_ACTIVE &&
            nearDestinationCount >= 3
        ) {
            TrackingState.NEAR_DESTINATION
        } else {
            currentState
        }
    }
}
