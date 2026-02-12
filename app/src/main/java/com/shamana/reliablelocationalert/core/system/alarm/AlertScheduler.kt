package com.shamana.reliablelocationalert.core.system.alarm

import com.shamana.reliablelocationalert.core.domain.model.AlertRequest

interface AlertScheduler {
    fun schedule(alert: AlertRequest)
    fun cancel()
}
