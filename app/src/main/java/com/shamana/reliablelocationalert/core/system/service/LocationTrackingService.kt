package com.shamana.reliablelocationalert.core.system.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.shamana.reliablelocationalert.core.domain.model.AlertRequest
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.usecase.ProcessLocationUpdateUseCase
import com.shamana.reliablelocationalert.core.system.alarm.AlarmManagerScheduler
import com.shamana.reliablelocationalert.core.system.location.FusedLocationProviderImpl
import com.shamana.reliablelocationalert.core.system.location.LocationProvider

class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    private val destination = Destination(
        latitude = 12.978330,
        longitude = 77.638489,
        alertRadiusMeters = 200f
    )

    private lateinit var locationProvider: LocationProvider
    private lateinit var processUseCase: ProcessLocationUpdateUseCase
    private lateinit var scheduler: AlarmManagerScheduler
    private var currentState = TrackingState.TRACKING_ACTIVE
    private var isTrackingActive = false

    override fun onCreate() {
        super.onCreate()

        locationProvider = FusedLocationProviderImpl(this)
        processUseCase = ProcessLocationUpdateUseCase(destination)
        scheduler = AlarmManagerScheduler(this)

        val savedStateName = getSharedPreferences("engine", MODE_PRIVATE)
            .getString("tracking_state", TrackingState.TRACKING_ACTIVE.name)

        currentState = try {
            TrackingState.valueOf(savedStateName!!)
        } catch (e: Exception) {
            TrackingState.TRACKING_ACTIVE
        }

        Log.d("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        if (!hasLocationPermission()) {
            Log.e("LocationService", "No location permission. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isSessionActive()) {
            Log.d("LocationService", "No active session. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isTrackingActive) {
            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            locationProvider.start { location ->

                val sample = LocationSample(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    timestampMillis = System.currentTimeMillis(),
                    speedMps = if (location.hasSpeed()) location.speed else null
                )

                val newState = processUseCase.process(sample, currentState)

                if (newState != currentState) {

                    Log.d("LocationService", "State changed: $currentState → $newState")

                    if (newState == TrackingState.NEAR_DESTINATION) {

                        scheduleArrivalAlert()

                        currentState = TrackingState.COMPLETED

                        getSharedPreferences("engine", MODE_PRIVATE)
                            .edit()
                            .putString("tracking_state", TrackingState.COMPLETED.name)
                            .apply()

                        locationProvider.stop()
                        stopSelf()

                    } else {

                        currentState = newState

                        getSharedPreferences("engine", MODE_PRIVATE)
                            .edit {
                                putString("tracking_state", newState.name)
                            }
                    }
                }
            }

            isTrackingActive = true
            Log.d("LocationService", "Location updates started")

        } catch (e: SecurityException) {
            Log.e("LocationService", "Permission revoked during tracking")
            stopSelf()
        }
    }

    private fun scheduleArrivalAlert() {

        val triggerTime = System.currentTimeMillis() + 2000

        scheduler.schedule(
            AlertRequest(
                triggerAtMillis = triggerTime,
                reason = "You are near your destination"
            )
        )

        Log.d("LocationService", "Arrival alert scheduled")
    }

    private fun isSessionActive(): Boolean {

        val prefs = getSharedPreferences("engine", MODE_PRIVATE)

        val active = prefs.getBoolean("tracking_active", false)

        val stateName = prefs.getString(
            "tracking_state",
            TrackingState.TRACKING_ACTIVE.name
        )

        val state = try {
            TrackingState.valueOf(stateName!!)
        } catch (e: Exception) {
            TrackingState.TRACKING_ACTIVE
        }

        return active && state != TrackingState.COMPLETED
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun createNotification(): Notification {
        val channelId = "location_tracking"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking location")
            .setContentText("Active background location tracking")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        locationProvider.stop()
        isTrackingActive = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
