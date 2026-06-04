package com.shamana.reliablelocationalert.core.system.permission

import com.shamana.reliablelocationalert.core.domain.model.Destination

/**
 * Result of the permission flow.
 */
sealed class PermissionState {

    data class ReadyToTrack(
        val destination: Destination
    ) : PermissionState()

    data class Denied(
        val message: String
    ) : PermissionState()

    object NeedsBackgroundLocationSettings : PermissionState()

    object NeedsExactAlarmSettings : PermissionState()
}
