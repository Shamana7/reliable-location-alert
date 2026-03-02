package com.shamana.reliablelocationalert.core.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shamana.reliablelocationalert.ReliableLocationAlertApp
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.system.service.LocationTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as ReliableLocationAlertApp
        val repository = app.container.trackingRepository

        CoroutineScope(Dispatchers.IO).launch {

            val session = repository.getSession()

            if (session != null && session.state != TrackingState.COMPLETED) {

                val serviceIntent =
                    Intent(context, LocationTrackingService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                Log.d("BootReceiver", "Tracking resumed after reboot")
            }
        }
    }
}
