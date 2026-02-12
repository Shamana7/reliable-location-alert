package com.shamana.reliablelocationalert.core.domain.model

data class AlertRequest(
    val triggerAtMillis: Long,
    val reason: String
)
