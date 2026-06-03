package com.shamana.reliablelocationalert

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.ui.presentation.TrackingViewModel
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingViewModel: TrackingViewModel? = null
    private var pendingDestination: Destination? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ReliableLocationAlertTheme {

                val viewModel: TrackingViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var latitude by remember(uiState.destination) {
                    mutableStateOf(uiState.destination?.latitude?.toString() ?: "")
                }

                var longitude by remember(uiState.destination) {
                    mutableStateOf(uiState.destination?.longitude?.toString() ?: "")
                }

                var radius by remember(uiState.destination) {
                    mutableStateOf(uiState.destination?.alertRadiusMeters?.toString() ?: "200")
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        if (uiState.isTracking) {

                            val isWaitingForGps =
                                uiState.isTracking &&
                                        uiState.lastLat == null &&
                                        uiState.lastLng == null

                            val screenTitle =
                                when (uiState.state) {

                                    TrackingState.ALERT_TRIGGERED ->
                                        "Destination Reached"

                                    TrackingState.TRACKING_DEGRADED ->
                                        "GPS Signal Weak"

                                    else ->
                                        "Tracking Active"
                                }

                            Text(
                                text = screenTitle,
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            if (isWaitingForGps) {

                                androidx.compose.material3.Card {

                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {

                                        Text(
                                            text = "📡 Waiting for GPS"
                                        )

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

                                        Text(
                                            text = "Trying to get your current location. This may take a few seconds."
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )
                            }

                            if (!isWaitingForGps) {
                                if (uiState.state == TrackingState.TRACKING_DEGRADED) {
                                    Text(
                                        text = "GPS signal weak. Trying to reconnect..."
                                    )
                                }

                                /* -------------------- Distance Card -------------------- */

                                androidx.compose.material3.Card {

                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {

                                        Text(
                                            text = "Distance Remaining",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                        )

                                       Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

                                        Text(
                                            text = "${uiState.distanceMeters?.toInt() ?: "--"} m",
                                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                }

                                androidx.compose.foundation.layout.Spacer(
                                    modifier = Modifier.height(20.dp)
                                )

                                /* -------------------- ETA -------------------- */

                                Text(
                                    text = "Estimated Arrival",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                )

                                val etaText = uiState.etaSeconds?.let { seconds ->

                                    val minutes = seconds / 60
                                    val remainingSeconds = seconds % 60

                                    if (minutes == 0L) {
                                        "$remainingSeconds sec"
                                    } else {
                                        "$minutes min $remainingSeconds sec"
                                    }

                                } ?: "--"

                                Text(
                                    text = etaText,
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )

                                /* -------------------- Live Location -------------------- */

                                androidx.compose.material3.Card {

                                    androidx.compose.foundation.layout.Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {

                                        Text(
                                            text = "Live Location",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                        )

                                        Spacer(
                                            modifier = Modifier.height(6.dp)
                                        )

                                        Text(text = "Lat: ${uiState.lastLat}")

                                        Text(
                                            text = "Lng: ${uiState.lastLng}"
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(20.dp)
                                )

                                /* -------------------- Progress -------------------- */

                                Text(
                                    text = when (uiState.state) {
                                        TrackingState.TRACKING_ACTIVE ->
                                            "Tracking in progress"

                                        TrackingState.TRACKING_DEGRADED ->
                                            "GPS signal weak"

                                        TrackingState.NEAR_DESTINATION ->
                                            "Almost there"

                                        TrackingState.ALERT_TRIGGERED ->
                                            "Destination reached"

                                        else ->
                                            uiState.state?.name ?: ""
                                    },
                                    modifier = Modifier.padding(bottom = 28.dp)
                                )

                                if (uiState.state == TrackingState.ALERT_TRIGGERED) {

                                    Text(
                                        text = "🎉 Arrived near destination!"
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Tracking completed successfully."
                                    )
                                }
                            }

                        } else {

                           OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude") }
                            )

                          Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude") }
                            )

                           Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                           OutlinedTextField(
                                value = radius,
                                onValueChange = { radius = it },
                                label = { Text("Radius (meters)") }
                            )
                        }

                        Button(
                            modifier = Modifier.padding(top = 20.dp),
                            onClick = {

                                if (uiState.isTracking) {

                                    viewModel.stopTracking()

                                } else {

                                    if (latitude.isBlank() || longitude.isBlank()) return@Button

                                    val lat = latitude.toDoubleOrNull()
                                    val lng = longitude.toDoubleOrNull()
                                    val rad = radius.toFloatOrNull()

                                    if (lat == null || lng == null || rad == null) {
                                        return@Button
                                    }

                                    val dest = Destination(
                                        latitude = lat,
                                        longitude = lng,
                                        alertRadiusMeters = rad
                                    )

                                    checkPermissionsAndStart(viewModel, dest)
                                }
                            }
                        ) {

                            Text(
                                if (uiState.isTracking)
                                    "Stop Tracking"
                                else
                                    "Start Tracking"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (hasLocationPermission() &&
            hasBackgroundLocationPermission()
        ) {

            pendingDestination?.let { dest ->
                pendingViewModel?.startTracking(dest)
            }
        }
    }

    /* -------------------- Permission Launchers -------------------- */

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) requestLocationPermission()
        }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !hasBackgroundLocationPermission()
                ) {

                    requestBackgroundLocationPermission()
                    return@registerForActivityResult
                }

                pendingDestination?.let { dest ->
                    pendingViewModel?.startTracking(dest)
                }
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                pendingDestination?.let { dest ->
                    pendingViewModel?.startTracking(dest)
                }

            } else {

                showBackgroundLocationSettingsDialog()
            }
        }

    private fun showBackgroundLocationSettingsDialog() {

        AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage(
                "Reliable Location Alert needs background location access so tracking continues when the app is minimized or the screen is off.\n\nPlease enable 'Allow all the time' in App Settings."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /* -------------------- Permission Flow -------------------- */

    private fun checkPermissionsAndStart(
        viewModel: TrackingViewModel,
        destination: Destination
    ) {

        pendingViewModel = viewModel
        pendingDestination = destination

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
            return
        }

        if (!hasExactAlarmPermission()) {
            requestExactAlarmPermission()
            return
        }

        if (hasLocationPermission()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !hasBackgroundLocationPermission()
            ) {

                requestBackgroundLocationPermission()
                return
            }

            viewModel.startTracking(destination)

        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            checkSelfPermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            true
        }
    }

    private fun requestBackgroundLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            backgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun openAppSettings() {

        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager =
                getSystemService(ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            )
            startActivity(intent)
        }
    }
}
