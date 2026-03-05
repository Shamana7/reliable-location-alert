package com.shamana.reliablelocationalert

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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

                            Text(
                                text = "Tracking Active",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            /* -------------------- Distance Card -------------------- */

                            androidx.compose.material3.Card {

                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {

                                    Text(
                                        text = "Distance Remaining",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                    )

                                    androidx.compose.foundation.layout.Spacer(
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

                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Text(text = "Lat: ${uiState.lastLat}")

                                    Text(
                                        text = "Lng: ${uiState.lastLng}"
                                    )
                                }
                            }

                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(20.dp)
                            )

                            /* -------------------- Progress -------------------- */

                            val progressPercent =
                                (uiState.progress?.times(100))?.toInt() ?: 0

                            Text(
                                text = "Progress $progressPercent%",
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                            )

                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            LinearProgressIndicator(
                                progress = { uiState.progress ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Text(
                                text = "Current State: ${uiState.state}",
                                modifier = Modifier.padding(bottom = 28.dp)
                            )

                        } else {

                            androidx.compose.material3.OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude") }
                            )

                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            androidx.compose.material3.OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude") }
                            )

                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            androidx.compose.material3.OutlinedTextField(
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
                pendingDestination?.let { dest ->
                    pendingViewModel?.startTracking(dest)
                }
            }
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
