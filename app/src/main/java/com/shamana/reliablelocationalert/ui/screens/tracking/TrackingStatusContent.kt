package com.shamana.reliablelocationalert.ui.screens.tracking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.model.TrackingUiState
import com.shamana.reliablelocationalert.ui.components.InfoCard
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme

/**
 * Everything shown while a tracking session is active.
 * Replaces the ~100-line inline `if (uiState.isTracking) { ... }` block
 * that previously lived directly in MainActivity's setContent.
 */
@Composable
fun TrackingStatusContent(
    uiState: TrackingUiState,
    modifier: Modifier = Modifier
) {
    val isWaitingForGps =
        uiState.lastLat == null && uiState.lastLng == null

    Column(modifier = modifier) {

        Text(
            text = TrackingFormatters.screenTitle(uiState.state),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (isWaitingForGps) {
            WaitingForGpsCard()
            Spacer(modifier = Modifier.height(16.dp))
            return@Column
        }

        if (uiState.state == TrackingState.TRACKING_DEGRADED) {
            Text(text = "GPS signal weak. Trying to reconnect...")
        }

        DistanceCard(uiState.distanceMeters)

        Spacer(modifier = Modifier.height(20.dp))

        EtaSection(uiState.etaSeconds)

        LiveLocationCard(uiState.lastLat, uiState.lastLng)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = TrackingFormatters.progressLabel(uiState.state),
            modifier = Modifier.padding(bottom = 28.dp)
        )

        if (uiState.state == TrackingState.ALERT_TRIGGERED) {
            ArrivedBanner()
        }
    }
}

@Composable
private fun WaitingForGpsCard(modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Text(text = "📡 Waiting for GPS")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Trying to get your current location. This may take a few seconds.")
    }
}

@Composable
private fun DistanceCard(distanceMeters: Float?, modifier: Modifier = Modifier) {
    InfoCard(title = "Distance Remaining", modifier = modifier) {
        Text(
            text = TrackingFormatters.distanceText(distanceMeters),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun EtaSection(etaSeconds: Long?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Estimated Arrival",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = TrackingFormatters.etaText(etaSeconds),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun LiveLocationCard(lat: Double?, lng: Double?, modifier: Modifier = Modifier) {
    InfoCard(title = "Live Location", modifier = modifier) {
        Text(text = "Lat: $lat")
        Text(text = "Lng: $lng")
    }
}

@Composable
private fun ArrivedBanner(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = "🎉 Arrived near destination!")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Tracking completed successfully.")
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingStatusContentPreview() {
    ReliableLocationAlertTheme {
        TrackingStatusContent(
            uiState = TrackingUiState(
                isTracking = true,
                state = TrackingState.TRACKING_ACTIVE,
                lastLat = 12.9716,
                lastLng = 77.5946,
                distanceMeters = 240f,
                etaSeconds = 96L
            )
        )
    }
}
