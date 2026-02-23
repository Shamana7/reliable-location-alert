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
import com.shamana.reliablelocationalert.core.system.location.FusedLocationProviderImpl
import com.shamana.reliablelocationalert.core.system.location.LocationProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var locationProvider: LocationProvider
    private var isTrackingActive = false

    override fun onCreate() {
        super.onCreate()
        locationProvider = FusedLocationProviderImpl(this)
        Log.d("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) {
            Log.d("LocationService", "Service restarted by system (intent=null)")
        }

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

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        if (!isTrackingActive) {
            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            locationProvider.start { location ->
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date())

                Log.d(
                    "LocationService",
                    "📍 [$time] Lat=${"%.6f".format(location.latitude)}, Lng=${"%.6f".format(location.longitude)}"
                )
            }
            isTrackingActive = true
            Log.d("LocationService", "Location updates started")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Permission revoked during tracking")
            stopSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("LocationService", "Task removed from recents")

        val restartIntent = Intent(applicationContext, LocationTrackingService::class.java)
        restartIntent.setPackage(packageName)
        startService(restartIntent)

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        locationProvider.stop()
        isTrackingActive = false
        Log.d("LocationService", "Service destroyed, location stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isSessionActive(): Boolean {
        val prefs = getSharedPreferences("engine", MODE_PRIVATE)
        return prefs.getBoolean("tracking_active", false)
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
}