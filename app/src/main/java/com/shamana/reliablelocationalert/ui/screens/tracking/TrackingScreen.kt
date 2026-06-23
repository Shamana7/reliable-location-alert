package com.shamana.reliablelocationalert.ui.screens.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingUiState
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme

/**
 * Root screen for the app.
 *
 * Deliberately stateless with respect to business logic: it takes a
 * [TrackingUiState] snapshot and emits user intent via callbacks
 * (onStartTracking / onStopTracking). The caller (MainActivity) owns the
 * ViewModel and the PermissionManager, since permission requests need a
 * ComponentActivity. This keeps the screen itself trivially previewable
 * and independent of Android framework/permission plumbing.
 */
@Composable
fun TrackingScreen(
    uiState: TrackingUiState,
    onStartTracking: (Destination) -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var latitude by remember(uiState.destination) {
        mutableStateOf(uiState.destination?.latitude?.toString() ?: "")
    }
    var longitude by remember(uiState.destination) {
        mutableStateOf(uiState.destination?.longitude?.toString() ?: "")
    }
    var radius by remember(uiState.destination) {
        mutableStateOf(uiState.destination?.alertRadiusMeters?.toString() ?: "200")
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isTracking) {
                TrackingStatusContent(uiState = uiState)
            } else {
                DestinationSetupForm(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    onLatitudeChange = { latitude = it },
                    onLongitudeChange = { longitude = it },
                    onRadiusChange = { radius = it },
                    errorMessage = uiState.errorMessage
                )
            }

            Button(
                modifier = Modifier.padding(top = 20.dp),
                onClick = {
                    if (uiState.isTracking) {
                        onStopTracking()
                    } else {
                        val lat = latitude.toDoubleOrNull()
                        val lng = longitude.toDoubleOrNull()
                        val rad = radius.toFloatOrNull()

                        if (lat != null && lng != null && rad != null) {
                            onStartTracking(Destination(lat, lng, rad))
                        }
                    }
                }
            ) {
                Text(if (uiState.isTracking) "Stop Tracking" else "Start Tracking")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingScreenSetupPreview() {
    ReliableLocationAlertTheme {
        TrackingScreen(
            uiState = TrackingUiState(),
            onStartTracking = {},
            onStopTracking = {}
        )
    }
}
