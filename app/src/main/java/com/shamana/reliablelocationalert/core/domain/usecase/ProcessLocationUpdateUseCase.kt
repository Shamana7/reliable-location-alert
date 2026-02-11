package com.shamana.reliablelocationalert.core.domain.usecase

import com.shamana.reliablelocationalert.core.domain.model.*

class ProcessLocationUpdateUseCase(
    private val destination: Destination
) {

    private val buffer = LocationSampleBuffer()
    private var nearDestinationCount = 0

    fun process(
        sample: LocationSample,
        currentState: TrackingState
    ): TrackingState {

        buffer.add(sample)

        if (!buffer.isFull()) {
            return currentState
        }

        val samples = buffer.samples()

        val avgDistance =
            DistanceCalculator.averageDistanceMeters(samples, destination)

        val etaSeconds =
            EtaEstimator.estimateSeconds(avgDistance, samples)

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
