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
import com.shamana.reliablelocationalert.ReliableLocationAlertApp
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepository
import com.shamana.reliablelocationalert.core.domain.model.AlertRequest
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.usecase.ProcessLocationUpdateUseCase
import com.shamana.reliablelocationalert.core.system.alarm.AlarmManagerScheduler
import com.shamana.reliablelocationalert.core.system.location.FusedLocationProviderImpl
import com.shamana.reliablelocationalert.core.system.location.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var repository: TrackingRepository
    private lateinit var locationProvider: LocationProvider
    private lateinit var processUseCase: ProcessLocationUpdateUseCase
    private lateinit var scheduler: AlarmManagerScheduler

    private var currentState = TrackingState.TRACKING_ACTIVE
    private var isTrackingActive = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        repository =
            (application as ReliableLocationAlertApp)
                .container
                .trackingRepository

        scheduler = AlarmManagerScheduler(this)

        Log.d("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {

            val session = withContext(Dispatchers.IO) {
                repository.getSession()
            }

            if (session == null || session.state == TrackingState.COMPLETED) {
                Log.d("LocationService", "No active session. Stopping.")
                stopSelf()
                return@launch
            }

            currentState = session.state

            initializeTracking(session.destination)
        }

        return START_STICKY
    }

    private fun initializeTracking(destination: Destination) {

        locationProvider = FusedLocationProviderImpl(this)
        processUseCase = ProcessLocationUpdateUseCase(destination)

        if (!isTrackingActive) {
            startLocationUpdates()
        }
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

                    serviceScope.launch(Dispatchers.IO) {

                        val session = repository.getSession()

                        if (session != null) {

                            repository.saveSession(
                                session.copy(
                                    state = newState,
                                    lastKnownLatitude = sample.latitude,
                                    lastKnownLongitude = sample.longitude,
                                    lastUpdatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    if (newState == TrackingState.NEAR_DESTINATION) {

                        scheduleArrivalAlert()

                        currentState = TrackingState.COMPLETED

                        locationProvider.stop()
                        stopSelf()

                    } else {
                        currentState = newState
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
        serviceScope.cancel()

        if (::locationProvider.isInitialized) {
            locationProvider.stop()
        }

        isTrackingActive = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}