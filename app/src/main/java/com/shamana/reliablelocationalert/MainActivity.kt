package com.shamana.reliablelocationalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.system.permission.PermissionManager
import com.shamana.reliablelocationalert.core.system.permission.PermissionState
import com.shamana.reliablelocationalert.ui.presentation.TrackingViewModel
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager.register(this)

        enableEdgeToEdge()
        setContent {
            ReliableLocationAlertTheme {

                val viewModel: TrackingViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                permissionManager.onResult = { state ->
                    when (state) {
                        is PermissionState.ReadyToTrack -> {
                            viewModel.startTracking(state.destination)
                        }
                        is PermissionState.Denied -> {
                            viewModel.showError(state.message)
                        }
                        is PermissionState.NeedsBackgroundLocationSettings -> {
                        }
                        is PermissionState.NeedsExactAlarmSettings -> {
                        }
                    }
                }

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

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        if (uiState.isTracking) {

                            val isWaitingForGps =
                                uiState.isTracking && uiState.lastLat == null && uiState.lastLng == null

                            val screenTitle = when (uiState.state) {
                                TrackingState.ALERT_TRIGGERED -> "Destination Reached"
                                TrackingState.TRACKING_DEGRADED -> "GPS Signal Weak"
                                else -> "Tracking Active"
                            }

                            Text(
                                text = screenTitle,
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            if (isWaitingForGps) {
                                Card {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "📡 Waiting for GPS")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = "Trying to get your current location. This may take a few seconds.")
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (!isWaitingForGps) {

                                if (uiState.state == TrackingState.TRACKING_DEGRADED) {
                                    Text(text = "GPS signal weak. Trying to reconnect...")
                                }

                                /* -------------------- Distance Card -------------------- */

                                Card {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = "Distance Remaining",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (
                                                uiState.distanceMeters == null ||
                                                uiState.distanceMeters == Float.MAX_VALUE
                                            ) "--" else "${uiState.distanceMeters?.toInt()} m",
                                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                /* -------------------- ETA -------------------- */

                                Text(
                                    text = "Estimated Arrival",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                )

                                val etaText = uiState.etaSeconds?.let { seconds ->
                                    val minutes = seconds / 60
                                    val remainingSeconds = seconds % 60
                                    if (minutes == 0L) "$remainingSeconds sec"
                                    else "$minutes min $remainingSeconds sec"
                                } ?: "--"

                                Text(
                                    text = etaText,
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )

                                /* -------------------- Live Location -------------------- */

                                Card {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Live Location",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = "Lat: ${uiState.lastLat}")
                                        Text(text = "Lng: ${uiState.lastLng}")
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                /* -------------------- Progress -------------------- */

                                Text(
                                    text = when (uiState.state) {
                                        TrackingState.TRACKING_ACTIVE -> "Tracking in progress"
                                        TrackingState.TRACKING_DEGRADED -> "GPS signal weak"
                                        TrackingState.NEAR_DESTINATION -> "Almost there"
                                        TrackingState.ALERT_TRIGGERED -> "Destination reached"
                                        else -> uiState.state?.name ?: ""
                                    },
                                    modifier = Modifier.padding(bottom = 28.dp)
                                )

                                if (uiState.state == TrackingState.ALERT_TRIGGERED) {
                                    Text(text = "🎉 Arrived near destination!")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Tracking completed successfully.")
                                }
                            }

                        } else {

                            uiState.errorMessage?.let { error ->
                                Card {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = "⚠️ Permission Required")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude") }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude") }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

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

                                    if (lat == null || lng == null || rad == null) return@Button

                                    val dest = Destination(
                                        latitude = lat,
                                        longitude = lng,
                                        alertRadiusMeters = rad
                                    )

                                    permissionManager.requestAll(dest, this@MainActivity)
                                }
                            }
                        ) {
                            Text(if (uiState.isTracking) "Stop Tracking" else "Start Tracking")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionManager.onActivityResumed(this)
    }
}