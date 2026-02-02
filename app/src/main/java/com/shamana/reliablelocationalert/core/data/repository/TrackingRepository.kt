package com.shamana.reliablelocationalert.core.data.repository

import com.shamana.reliablelocationalert.core.domain.model.TrackingSession

interface TrackingRepository {
    suspend fun getSession(): TrackingSession?
    suspend fun saveSession(session: TrackingSession)
    suspend fun clear()
}
