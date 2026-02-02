package com.shamana.reliablelocationalert.core.domain.model

enum class TrackingState {
    IDLE,                   // No active session
    TRACKING_ACTIVE,        // Receiving location updates
    TRACKING_DEGRADED,      // OS constraints / poor signal
    NEAR_DESTINATION,       // Prediction threshold crossed
    ALERT_TRIGGERED,        // Alert fired successfully
    COMPLETED,              // User acknowledged / session done
    ERROR_RECOVERABLE       // Permission/network/temp failure
}
