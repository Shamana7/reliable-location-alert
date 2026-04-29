package com.shamana.reliablelocationalert.core.system.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isBatteryOptimizationEnabled(
        context: Context
    ): Boolean {

        val powerManager =
            context.getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        return !powerManager.isIgnoringBatteryOptimizations(
            context.packageName
        )
    }

    fun requestDisableBatteryOptimization(
        context: Context
    ) {

        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        ).apply {

            data = Uri.parse(
                "package:${context.packageName}"
            )

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
    }
}