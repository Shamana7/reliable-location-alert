package com.shamana.reliablelocationalert.ui.screens.tracking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shamana.reliablelocationalert.ui.components.InfoCard
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme

/**
 * Destination input form shown before tracking starts.
 * Pure/stateless: the screen owns the field values and passes them in,
 * so this composable is trivially previewable and testable in isolation.
 */
@Composable
fun DestinationSetupForm(
    latitude: String,
    longitude: String,
    radius: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        errorMessage?.let { error ->
            InfoCard(title = "⚠️ Permission Required") {
                Text(text = error)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = latitude,
            onValueChange = onLatitudeChange,
            label = { Text("Latitude") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = longitude,
            onValueChange = onLongitudeChange,
            label = { Text("Longitude") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = radius,
            onValueChange = onRadiusChange,
            label = { Text("Radius (meters)") }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DestinationSetupFormPreview() {
    ReliableLocationAlertTheme {
        DestinationSetupForm(
            latitude = "12.9716",
            longitude = "77.5946",
            radius = "200",
            onLatitudeChange = {},
            onLongitudeChange = {},
            onRadiusChange = {},
            errorMessage = "Location permission is required to start tracking."
        )
    }
}
