package com.shamana.reliablelocationalert

import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.usecase.EtaEstimator
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class EtaEstimatorTest {

    @Test
    fun estimate_eta_uses_average_speed() {

        val samples = listOf(
            LocationSample(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = 10f,
                timestampMillis = 0L,
                speedMps = 2f
            ),
            LocationSample(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = 10f,
                timestampMillis = 0L,
                speedMps = 2f
            )
        )

        val eta = EtaEstimator.estimateSeconds(
            distanceMeters = 100f,
            samples = samples
        )

        assertEquals(50L, eta)
    }

    @Test
    fun uses_fallback_speed_when_no_speed_samples_exist() {

        val samples = listOf(
            LocationSample(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = 10f,
                timestampMillis = 0L,
                speedMps = null
            )
        )

        val eta = EtaEstimator.estimateSeconds(
            distanceMeters = 120f,
            samples = samples
        )

        assertTrue(eta in 99L..100L)
    }

    @Test
    fun clamps_speed_to_minimum_value() {

        val samples = listOf(
            LocationSample(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = 10f,
                timestampMillis = 0L,
                speedMps = 0.1f
            )
        )

        val eta = EtaEstimator.estimateSeconds(
            distanceMeters = 50f,
            samples = samples
        )

        assertEquals(100L, eta)
    }

    @Test
    fun clamps_speed_to_maximum_value() {

        val samples = listOf(
            LocationSample(
                latitude = 0.0,
                longitude = 0.0,
                accuracyMeters = 10f,
                timestampMillis = 0L,
                speedMps = 10f
            )
        )

        val eta = EtaEstimator.estimateSeconds(
            distanceMeters = 300f,
            samples = samples
        )

        assertEquals(100L, eta)
    }
}