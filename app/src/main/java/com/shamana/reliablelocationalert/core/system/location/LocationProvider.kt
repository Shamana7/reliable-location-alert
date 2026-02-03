package com.shamana.reliablelocationalert.core.system.location

import android.location.Location

interface LocationProvider {
    fun start(onLocation: (Location) -> Unit)
    fun stop()
}
