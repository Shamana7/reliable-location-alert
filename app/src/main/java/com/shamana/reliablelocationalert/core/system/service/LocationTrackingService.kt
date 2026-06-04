package com.shamana.reliablelocationalert.core.system.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepository
import com.shamana.reliablelocationalert.core.domain.model.AlertRequest
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.LocationSample
import com.shamana.reliablelocationalert.core.domain.model.TrackingSession
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.usecase.DistanceCalculator
import com.shamana.reliablelocationalert.core.domain.usecase.EtaEstimator
import com.shamana.reliablelocationalert.core.domain.usecase.LocationSampleBuffer
import com.shamana.reliablelocationalert.core.domain.usecase.ProcessLocationUpdateUseCase
import com.shamana.reliablelocationalert.core.system.alarm.AlarmManagerScheduler
import com.shamana.reliablelocationalert.core.system.location.LocationProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
    }

    @Inject
    lateinit var repository: TrackingRepository

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var scheduler: AlarmManagerScheduler

    private lateinit var processUseCase: ProcessLocationUpdateUseCase

    private var currentState = TrackingState.TRACKING_ACTIVE
    private var isTrackingActive = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sampleBuffer = LocationSampleBuffer(5)
    private var activeSession: TrackingSession? = null
    private var startDistance: Float? = null

    private var lastLocationUpdateTime = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()

        Log.d("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP_TRACKING) {

            serviceScope.launch(Dispatchers.IO) {

                repository.clear()
            }

            stopSelf()

            return START_NOT_STICKY
        }

        if (!hasLocationPermission()) {

            Log.e(
                "LocationService",
                "Location permission missing. Stopping service."
            )

            serviceScope.launch(Dispatchers.IO) {
                repository.clear()
            }

            stopSelf()

            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        serviceScope.launch {

            val session = withContext(Dispatchers.IO) {
                repository.getSession()
            }

            activeSession = session

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

        processUseCase = ProcessLocationUpdateUseCase(destination)

        if (!isTrackingActive) {
            startLocationUpdates()
            startGpsWatchdog()
        }
    }

    private fun startLocationUpdates() {

        try {
            locationProvider.start { location ->

                lastLocationUpdateTime = System.currentTimeMillis()

                if (currentState == TrackingState.TRACKING_DEGRADED) {

                    activeSession?.let { session ->

                        val recovered = session.copy(
                            state = TrackingState.TRACKING_ACTIVE
                        )

                        activeSession = recovered
                        currentState = TrackingState.TRACKING_ACTIVE

                        serviceScope.launch(Dispatchers.IO) {
                            repository.saveSession(recovered)
                        }
                    }
                }

                val sample = LocationSample(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    timestampMillis = System.currentTimeMillis(),
                    speedMps = if (location.hasSpeed()) location.speed else null
                )

                val newState = processUseCase.process(sample, currentState)

                sampleBuffer.add(sample)

                activeSession?.let { session ->

                    val distance = DistanceCalculator.averageDistanceMeters(
                        sampleBuffer.samples(),
                        session.destination
                    )

                    if (startDistance == null) {
                        startDistance = distance
                    }

                    val progress = startDistance?.let { start ->
                        if (start > 0f) ((start - distance) / start).coerceIn(0f, 1f) else 0f
                    }

                    val eta = EtaEstimator.estimateSeconds(
                        distance,
                        sampleBuffer.samples()
                    )

                    val finalState =
                        if (
                            newState == TrackingState.NEAR_DESTINATION &&
                            scheduleArrivalAlert()
                        ) {
                            TrackingState.ALERT_TRIGGERED
                        } else {
                            newState
                        }

                    val updated = session.copy(
                        state = finalState,
                        lastKnownLatitude = sample.latitude,
                        lastKnownLongitude = sample.longitude,
                        lastUpdatedAt = System.currentTimeMillis(),
                        distanceMeters = distance,
                        etaSeconds = eta,
                        progress = progress
                    )

                    activeSession = updated

                    serviceScope.launch(Dispatchers.IO) {
                        repository.saveSession(updated)
                    }
                    if (finalState != currentState) {

                        Log.d(
                            "LocationService",
                            "State changed: $currentState → $finalState"
                        )

                        currentState = finalState

                        if (finalState == TrackingState.ALERT_TRIGGERED) {

                            locationProvider.stop()
                            stopSelf()
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

    private fun startGpsWatchdog() {

        serviceScope.launch {

            while (kotlinx.coroutines.currentCoroutineContext().isActive) {

                delay(30_000)

                val secondsSinceLastUpdate =
                    (System.currentTimeMillis() - lastLocationUpdateTime) / 1000

                if (
                    secondsSinceLastUpdate >= 90 &&
                    currentState != TrackingState.ALERT_TRIGGERED
                ) {

                    Log.d(
                        "LocationService",
                        "No GPS updates for $secondsSinceLastUpdate seconds"
                    )

                    activeSession?.let { session ->

                        val degraded = session.copy(
                            state = TrackingState.TRACKING_DEGRADED
                        )

                        activeSession = degraded
                        currentState = TrackingState.TRACKING_DEGRADED

                        serviceScope.launch(Dispatchers.IO) {
                            repository.saveSession(degraded)
                        }
                    }
                }
            }
        }
    }

    private fun scheduleArrivalAlert(): Boolean {

        val triggerTime = System.currentTimeMillis() + 2000

        return try {
            scheduler.schedule(
                AlertRequest(
                    triggerAtMillis = triggerTime,
                    reason = "You are near your destination"
                )
            )
            Log.d("LocationService", "Arrival alert scheduled")
            true
        } catch (e: SecurityException) {
            Log.e("LocationService", "Exact alarm scheduling failed")
            false
        }
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

        val stopIntent = Intent(
            this,
            LocationTrackingService::class.java
        ).apply {
            action = ACTION_STOP_TRACKING
        }

        val stopPendingIntent =
            PendingIntent.getService(
                this,
                100,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking location")
            .setContentText("Active background location tracking")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent
            )

        return builder.build()
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