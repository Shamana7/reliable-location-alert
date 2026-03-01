package com.shamana.reliablelocationalert.ui.presentation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shamana.reliablelocationalert.ReliableLocationAlertApp
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingSession
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.model.TrackingUiState
import com.shamana.reliablelocationalert.core.system.service.LocationTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        (application as ReliableLocationAlertApp)
            .container
            .trackingRepository

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState

    init {
        viewModelScope.launch {
            val session = repository.getSession()

            if (session != null && session.state != TrackingState.COMPLETED) {
                _uiState.value = TrackingUiState(
                    isTracking = true,
                    destination = session.destination,
                    state = session.state
                )
            }
        }
    }

    fun startTracking(destination: Destination) {

        viewModelScope.launch {

            repository.saveSession(
                TrackingSession(
                    destination = destination,
                    state = TrackingState.TRACKING_ACTIVE,
                    lastKnownLatitude = null,
                    lastKnownLongitude = null,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            )

            val intent = Intent(context, LocationTrackingService::class.java)
            context.startForegroundService(intent)

            _uiState.value = _uiState.value.copy(isTracking = true)
        }
    }

    fun stopTracking() {

        viewModelScope.launch {
            repository.clear()

            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)

            _uiState.value = _uiState.value.copy(isTracking = false)
        }
    }
}
