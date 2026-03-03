package com.shamana.reliablelocationalert.core.data.repository

import com.shamana.reliablelocationalert.core.domain.model.TrackingSession
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {

    fun observeSession(): Flow<TrackingSession?>

    suspend fun getSession(): TrackingSession?
    suspend fun saveSession(session: TrackingSession)
    suspend fun clear()
}
