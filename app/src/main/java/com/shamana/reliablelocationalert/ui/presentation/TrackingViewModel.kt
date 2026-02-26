package com.shamana.reliablelocationalert.ui.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.shamana.reliablelocationalert.core.domain.model.TrackingUiState
import com.shamana.reliablelocationalert.core.system.service.LocationTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TrackingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState

    fun startTracking() {
        context.getSharedPreferences("engine", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("tracking_active", true)
            .apply()

        val intent = Intent(context, LocationTrackingService::class.java)
        context.startForegroundService(intent)

        _uiState.value = _uiState.value.copy(isTracking = true)
    }

    fun stopTracking() {
        context.getSharedPreferences("engine", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("tracking_active", false)
            .apply()

        val intent = Intent(context, LocationTrackingService::class.java)
        context.stopService(intent)

        _uiState.value = _uiState.value.copy(isTracking = false)
    }
}
