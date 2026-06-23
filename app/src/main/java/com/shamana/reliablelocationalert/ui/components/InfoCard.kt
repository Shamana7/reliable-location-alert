package com.shamana.reliablelocationalert.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Reusable "label + content" card used across the tracking screen
 * (waiting-for-GPS, distance, live location, permission warning, arrival).
 *
 * Previously each of these was a hand-rolled Card { Column { ... } } block
 * duplicated inline in MainActivity.
 */
@Composable
fun InfoCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            title?.let {
                Text(text = it, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoCardPreview() {
    InfoCard(title = "Distance Remaining") {
        Text(text = "240 m", style = MaterialTheme.typography.headlineMedium)
    }
}
