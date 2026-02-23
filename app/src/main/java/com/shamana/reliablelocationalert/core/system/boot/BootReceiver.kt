package com.shamana.reliablelocationalert.core.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("engine", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("resume_required", true).apply()

        Log.d("BootReceiver", "Boot completed. Resume flag set.")
    }
}
