package com.shamana.reliablelocationalert.core.system.storage

import android.content.Context
import com.shamana.reliablelocationalert.core.domain.model.Destination

class DestinationStorage(
    private val context: Context
) {

    private val prefs =
        context.getSharedPreferences("engine", Context.MODE_PRIVATE)

    fun saveDestination(destination: Destination) {
        prefs.edit()
            .putFloat("dest_lat", destination.latitude.toFloat())
            .putFloat("dest_lng", destination.longitude.toFloat())
            .putFloat("dest_radius", destination.alertRadiusMeters)
            .apply()
    }

    fun getDestination(): Destination? {
        val lat = prefs.getFloat("dest_lat", Float.MIN_VALUE)
        val lng = prefs.getFloat("dest_lng", Float.MIN_VALUE)
        val radius = prefs.getFloat("dest_radius", 200f)

        if (lat == Float.MIN_VALUE || lng == Float.MIN_VALUE) {
            return null
        }

        return Destination(
            latitude = lat.toDouble(),
            longitude = lng.toDouble(),
            alertRadiusMeters = radius
        )
    }
}
