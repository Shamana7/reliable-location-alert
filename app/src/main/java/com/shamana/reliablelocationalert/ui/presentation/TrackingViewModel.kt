package com.shamana.reliablelocationalert.ui.presentation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shamana.reliablelocationalert.core.data.repository.TrackingRepository
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingSession
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import com.shamana.reliablelocationalert.core.domain.model.TrackingUiState
import com.shamana.reliablelocationalert.core.system.service.LocationTrackingService
import com.shamana.reliablelocationalert.core.system.util.BatteryOptimizationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val repository: TrackingRepository, private val application: Application
) : ViewModel() {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeSession().collect { session ->

                if (session == null) {
                    _uiState.value = TrackingUiState()
                } else {
                    _uiState.value = TrackingUiState(
                        isTracking = session.state != TrackingState.COMPLETED,
                        destination = session.destination,
                        state = session.state,
                        lastLat = session.lastKnownLatitude,
                        lastLng = session.lastKnownLongitude,
                        distanceMeters = session.distanceMeters,
                        etaSeconds = session.etaSeconds,
                        progress = session.progress
                    )
                }
            }
        }
    }

    fun startTracking(destination: Destination) {

        viewModelScope.launch {

            if (BatteryOptimizationHelper.isBatteryOptimizationEnabled(context)) {

                BatteryOptimizationHelper.requestDisableBatteryOptimization(context)

                return@launch
            }

            repository.saveSession(
                TrackingSession(
                    destination = destination,
                    state = TrackingState.TRACKING_ACTIVE,
                    lastKnownLatitude = null,
                    lastKnownLongitude = null,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            )

            val intent = Intent(
                context, LocationTrackingService::class.java
            )

            try {
                context.startForegroundService(intent)
            } catch (e: SecurityException) {

                showError(
                    "Location permission was revoked. Please grant permission again."
                )

                return@launch
            }

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

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message
        )
    }
}
