package com.shamana.reliablelocationalert.core.system.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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

    override fun onCreate() {
        super.onCreate()
        locationProvider = FusedLocationProviderImpl(this)
        Log.d("LocationService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        Log.d("LocationService", "Foreground started")

        locationProvider.start { location ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date())
            Log.d(
                "LocationService",
                "📍 [$time] Lat=${"%.6f".format(location.latitude)}, Lng=${"%.6f".format(location.longitude)}"
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        locationProvider.stop()
        Log.d("LocationService", "Service destroyed, location stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
