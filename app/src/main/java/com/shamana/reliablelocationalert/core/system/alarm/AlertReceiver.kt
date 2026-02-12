package com.shamana.reliablelocationalert.core.system.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission

class AlertReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val reason =
            intent.getStringExtra("reason") ?: "Destination near"

        AlertNotifier.show(context, reason)
    }
}
