package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Where the device is, as the rest of the app sees it.
 *
 * The same offline-first split as [MovieRepository], and for the same reason: only one of
 * the two operations can fail. Reading is local and always succeeds, returning the last
 * fix — possibly none. Acquiring a new one is separate, and a caller that ignores its
 * failure still has a map centred on somewhere real.
 */
interface LocationRepository {

    /**
     * The last fix written to the cache, re-emitted whenever a newer one replaces it.
     *
     * Never throws and never completes. Emits `null` until the device has ever been
     * located, which on a first run with the permission refused is forever.
     */
    fun observeLastKnownLocation(): Flow<UserLocation?>

    /**
     * Acquires a current fix and writes it to the cache.
     *
     * Deliberately returns nothing: the new position reaches the caller through
     * [observeLastKnownLocation], so there is exactly one path a position can arrive by and
     * no way for a screen to render a fix that was never cached.
     *
     * @throws com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
     *   if the location permission is not granted.
     * @throws com.example.test_ai_project.core.domain.exception.LocationUnavailableException
     *   if the platform has no position to give — location services off, or no provider
     *   has produced a fix yet.
     */
    suspend fun refreshCurrentLocation()
}
