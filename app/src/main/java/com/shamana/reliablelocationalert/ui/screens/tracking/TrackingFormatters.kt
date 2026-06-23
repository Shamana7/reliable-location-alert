package com.shamana.reliablelocationalert.ui.screens.tracking

import com.shamana.reliablelocationalert.core.domain.model.TrackingState

/**
 * Pure UI-formatting helpers, deliberately kept free of Compose/Android types
 * so they can be unit tested the same way EtaEstimator/ProcessLocationUpdateUseCase are.
 */
object TrackingFormatters {

    fun screenTitle(state: TrackingState?): String = when (state) {
        TrackingState.ALERT_TRIGGERED -> "Destination Reached"
        TrackingState.TRACKING_DEGRADED -> "GPS Signal Weak"
        else -> "Tracking Active"
    }

    fun distanceText(distanceMeters: Float?): String =
        if (distanceMeters == null || distanceMeters == Float.MAX_VALUE) {
            "--"
        } else {
            "${distanceMeters.toInt()} m"
        }

    fun etaText(etaSeconds: Long?): String {
        val seconds = etaSeconds ?: return "--"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes == 0L) "$remainingSeconds sec" else "$minutes min $remainingSeconds sec"
    }

    fun progressLabel(state: TrackingState?): String = when (state) {
        TrackingState.TRACKING_ACTIVE -> "Tracking in progress"
        TrackingState.TRACKING_DEGRADED -> "GPS signal weak"
        TrackingState.NEAR_DESTINATION -> "Almost there"
        TrackingState.ALERT_TRIGGERED -> "Destination reached"
        else -> state?.name ?: ""
    }
}
