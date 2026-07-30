package com.example.test_ai_project.home.domain.service

import com.example.test_ai_project.home.domain.model.MapCamera
import com.example.test_ai_project.home.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Where the user is, and where they last left the map looking.
 *
 * The two belong together because they are the same question asked twice — "what should
 * the map show when it opens?" is answered by the saved camera if there is one and the
 * last known fix otherwise. Splitting them would mean every caller wiring both.
 *
 * Shared by the map, weather and prayer-times tabs, which is why it is a service of the
 * home feature rather than of any one screen.
 */
interface LocationService {

    /**
     * The last fix, from cache. Emits `null` until there has ever been one.
     *
     * A `Flow` rather than a suspending read: the map redraws as the fix improves, and a
     * one-shot read would freeze it at whatever was cached when the screen opened.
     */
    fun observeLastKnownLocation(): Flow<UserLocation?>

    /** Asks the platform for a new fix and caches it. */
    suspend fun refreshCurrentLocation()

    /** The camera the user last left the map at, or `null` on first run. */
    suspend fun lastCamera(): MapCamera?

    suspend fun saveCamera(camera: MapCamera)
}
