package com.shamana.reliablelocationalert.core.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.shamana.reliablelocationalert.core.domain.model.AlertRequest

class AlarmManagerScheduler(
    private val context: Context
) : AlertScheduler {

    override fun schedule(alert: AlertRequest) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlertReceiver::class.java).apply {
            putExtra("reason", alert.reason)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alert.triggerAtMillis,
            pendingIntent
        )
    }

    override fun cancel() {
        val intent = Intent(context, AlertReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }
}
