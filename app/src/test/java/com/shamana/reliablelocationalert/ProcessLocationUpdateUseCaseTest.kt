package com.shamana.reliablelocationalert

import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.usecase.ProcessLocationUpdateUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessLocationUpdateUseCaseTest {

    private val destination = Destination(
        latitude = 12.9716,
        longitude = 77.5946,
        alertRadiusMeters = 100f
    )

    @Test
    fun active_to_degraded_after_3_bad_accuracy_samples() {

        val useCase = ProcessLocationUpdateUseCase(destination)

        var state = TrackingState.TRACKING_ACTIVE

        repeat(3) {
            state = useCase.process(
                sample = LocationSample(
                    latitude = 12.9716,
                    longitude = 77.5946,
                    accuracyMeters = 600f,
                    timestampMillis = System.currentTimeMillis(),
                    speedMps = null
                ),
                currentState = state
            )
        }

        assertEquals(
            TrackingState.TRACKING_DEGRADED,
            state
        )
    }

    @Test
    fun degraded_to_active_after_3_good_accuracy_samples() {

        val useCase = ProcessLocationUpdateUseCase(destination)

        var state = TrackingState.TRACKING_DEGRADED

        repeat(3) {
            state = useCase.process(
                sample = LocationSample(
                    latitude = 12.9716,
                    longitude = 77.5946,
                    accuracyMeters = 50f,
                    timestampMillis = System.currentTimeMillis(),
                    speedMps = null
                ),
                currentState = state
            )
        }

        assertEquals(
            TrackingState.TRACKING_ACTIVE,
            state
        )
    }
}
