package com.shamana.reliablelocationalert.core.system.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.HandlerThread
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class FusedLocationProviderImpl(
    private val context: Context
) : LocationProvider {

    private val client =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var locationThread: HandlerThread? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun start(onLocation: (Location) -> Unit) {

        if (locationThread == null) {
            locationThread = HandlerThread("LocationUpdates").apply {
                start()
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000L
        )
            .setMinUpdateIntervalMillis(10_000L) // OS can give faster if needed
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocation(location)
                }
            }
        }

        client.requestLocationUpdates(
            request,
            locationCallback!!,
            locationThread!!.looper
        )
    }

    override fun stop() {
        locationCallback?.let {
            client.removeLocationUpdates(it)
        }
        locationCallback = null

        locationThread?.quitSafely()
        locationThread = null
    }
}
