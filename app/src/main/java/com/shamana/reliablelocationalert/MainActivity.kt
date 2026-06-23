package com.shamana.reliablelocationalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shamana.reliablelocationalert.core.system.permission.PermissionManager
import com.shamana.reliablelocationalert.core.system.permission.PermissionState
import com.shamana.reliablelocationalert.ui.presentation.TrackingViewModel
import com.shamana.reliablelocationalert.ui.screens.tracking.TrackingScreen
import com.shamana.reliablelocationalert.ui.theme.ReliableLocationAlertTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the single Compose screen and owns the pieces that genuinely
 * require a ComponentActivity: permission requests/settings redirects
 * and the activity lifecycle callback that resumes the permission chain.
 *
 * All actual UI now lives in ui/screens/tracking — this class is just glue.
 */
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
                        is PermissionState.NeedsBackgroundLocationSettings -> Unit
                        is PermissionState.NeedsExactAlarmSettings -> Unit
                    }
                }

                TrackingScreen(
                    uiState = uiState,
                    onStartTracking = { destination ->
                        permissionManager.requestAll(destination, this@MainActivity)
                    },
                    onStopTracking = { viewModel.stopTracking() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionManager.onActivityResumed(this)
    }
}
