package com.shamana.reliablelocationalert.core.system.permission

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.shamana.reliablelocationalert.core.domain.model.Destination

/**
 * Coordinates the app's permission flow and settings redirects.
 */
class PermissionManager(
    private val context: Context
) {

    var onResult: ((PermissionState) -> Unit)? = null

    private var pendingDestination: Destination? = null

    // Used to resume permission validation after returning from Settings.
    private var waitingForSettingsReturn = false

    private lateinit var notificationLauncher: ActivityResultLauncher<String>
    private lateinit var locationLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationLauncher: ActivityResultLauncher<String>

    /**
     * Must be called before the Activity reaches STARTED state.
     */
    fun register(activity: ComponentActivity) {

        notificationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                proceedAfterNotification(activity)
            } else {
                onResult?.invoke(
                    PermissionState.Denied(
                        "Notification permission is required for arrival alerts."
                    )
                )
            }
        }

        locationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                proceedAfterForegroundLocation(activity)
            } else {
                onResult?.invoke(
                    PermissionState.Denied(
                        "Location permission is required to start tracking."
                    )
                )
            }
        }

        backgroundLocationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                dispatchReady()
            } else {
                showBackgroundLocationSettingsDialog(activity)
            }
        }
    }

    fun requestAll(
        destination: Destination,
        activity: ComponentActivity
    ) {
        pendingDestination = destination
        startPermissionChain(activity)
    }

    fun onActivityResumed(activity: ComponentActivity) {
        if (!waitingForSettingsReturn) return

        waitingForSettingsReturn = false

        if (hasLocationPermission() && hasBackgroundLocationPermission()) {
            dispatchReady()
        }
    }

    private fun startPermissionChain(activity: ComponentActivity) {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return
        }

        proceedAfterNotification(activity)
    }

    private fun proceedAfterNotification(activity: ComponentActivity) {

        if (!hasExactAlarmPermission()) {
            requestExactAlarmPermission(activity)
            return
        }

        proceedAfterExactAlarm(activity)
    }

    private fun proceedAfterExactAlarm(activity: ComponentActivity) {

        if (!hasLocationPermission()) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        proceedAfterForegroundLocation(activity)
    }

    private fun proceedAfterForegroundLocation(activity: ComponentActivity) {

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasBackgroundLocationPermission()
        ) {
            backgroundLocationLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            return
        }

        dispatchReady()
    }

    private fun dispatchReady() {
        pendingDestination?.let { destination ->
            onResult?.invoke(
                PermissionState.ReadyToTrack(destination)
            )
            pendingDestination = null
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            return alarmManager.canScheduleExactAlarms()
        }

        return true
    }

    private fun requestExactAlarmPermission(
        activity: ComponentActivity
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            waitingForSettingsReturn = true

            activity.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            )
        }
    }

    private fun showBackgroundLocationSettingsDialog(
        activity: ComponentActivity
    ) {
        AlertDialog.Builder(activity)
            .setTitle("Background location required")
            .setMessage(
                "Reliable Location Alert needs background location access to continue tracking while the app is in the background.\n\n" +
                        "Please follow these steps:\n\n" +
                        "1. Tap Permissions\n" +
                        "2. Tap Location\n" +
                        "3. Select 'Allow all the time'\n\n" +
                        "Then return to the app."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                waitingForSettingsReturn = true
                openAppSettings(activity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings(activity: ComponentActivity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent)
    }
}
