package com.shamana.reliablelocationalert.core.data.repository

import com.shamana.reliablelocationalert.core.data.local.TrackingDao
import com.shamana.reliablelocationalert.core.data.local.TrackingEntity
import com.shamana.reliablelocationalert.core.domain.model.Destination
import com.shamana.reliablelocationalert.core.domain.model.TrackingSession
import com.shamana.reliablelocationalert.core.domain.model.TrackingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackingRepositoryImpl (
    private val dao: TrackingDao
    ): TrackingRepository {

    override fun observeSession(): Flow<TrackingSession?> {
        return dao.observeSession().map { entity ->
            entity?.let {
                TrackingSession(
                    destination = Destination(
                        it.destinationLat,
                        it.destinationLng,
                        it.radius
                    ),
                    state = TrackingState.valueOf(it.state),
                    lastKnownLatitude = it.lastLat,
                    lastKnownLongitude = it.lastLng,
                    lastUpdatedAt = it.updatedAt
                )
            }
        }
    }

    override suspend fun getSession(): TrackingSession? {
        return dao.getSession()?.let {
            TrackingSession(
                destination = Destination(
                    it.destinationLat,
                    it.destinationLng,
                    it.radius
                ),
                state = TrackingState.valueOf(it.state),
                lastKnownLatitude = it.lastLat,
                lastKnownLongitude = it.lastLng,
                lastUpdatedAt = it.updatedAt
            )
        }
    }

    override suspend fun saveSession(session: TrackingSession) {
        dao.saveSession(
            TrackingEntity(
                destinationLat = session.destination.latitude,
                destinationLng = session.destination.longitude,
                radius = session.destination.alertRadiusMeters,
                state = session.state.name,
                lastLat = session.lastKnownLatitude,
                lastLng = session.lastKnownLongitude,
                updatedAt = session.lastUpdatedAt
            )
        )
    }

    override suspend fun clear() {
        dao.clear()
    }
}
