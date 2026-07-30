package com.example.test_ai_project.home.data.service

import com.example.test_ai_project.home.data.local.FusedLocationSource
import com.example.test_ai_project.home.data.mapper.toDomain
import com.example.test_ai_project.home.data.mapper.toEntity
import com.example.test_ai_project.database.dao.LocationDao
import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.exception.LocationUnavailableException
import com.example.test_ai_project.home.domain.service.LocationService
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.MapCamera
import com.example.test_ai_project.home.domain.model.UserLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first in the same strict sense as the movie cache: Room is the only thing the map
 * ever reads from, and the location provider is a process that writes into Room. A refresh
 * that fails leaves the previous fix on screen rather than blanking it.
 */
@Singleton
class DefaultLocationService @Inject constructor(
    private val locationDao: LocationDao,
    private val locationSource: FusedLocationSource,
    private val timeProvider: TimeProvider,
) : LocationService {

    override fun observeLastKnownLocation(): Flow<UserLocation?> =
        locationDao.observeLastKnownLocation().map { entity -> entity?.toDomain() }

    override suspend fun refreshCurrentLocation() = withContext(Dispatchers.IO) {
        val fix = try {
            // Falls back to whatever Play services already had: a fix from ten minutes ago
            // still centres the map correctly, and is what the user gets indoors.
            locationSource.currentLocation() ?: locationSource.lastLocation()
        } catch (denied: SecurityException) {
            // Translated at the layer boundary — this module is the only one that knows the
            // fix came from Play services, so it is the only one that can turn the
            // platform's exception into something the screen above can act on.
            throw LocationPermissionDeniedException(cause = denied)
        } catch (cancellation: CancellationException) {
            // Rethrown ahead of the catch-all below: leaving the screen cancels this
            // coroutine, and swallowing that into a "location unavailable" error would
            // report a failure for something the user did on purpose.
            throw cancellation
        } catch (failure: Exception) {
            // Everything else Play services can raise — most commonly a device with no
            // Google Play services at all, which is a permanent version of "unavailable".
            throw LocationUnavailableException(cause = failure)
        }

        // Null rather than an exception is the provider's way of saying location services
        // are off. It is the same story for the user as a failed request.
        if (fix == null) throw LocationUnavailableException()

        locationDao.upsertLastKnownLocation(
            fix.toEntity(fallbackEpochMillis = timeProvider.nowEpochMillis()),
        )
    }

    override suspend fun lastCamera(): MapCamera? = withContext(Dispatchers.IO) {
        locationDao.mapCamera()?.toDomain()
    }

    override suspend fun saveCamera(camera: MapCamera) = withContext(Dispatchers.IO) {
        locationDao.upsertMapCamera(camera.toEntity())
    }
}
