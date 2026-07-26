package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.MapCamera

/**
 * The map's saved viewport.
 *
 * Its own interface rather than two more methods on [LocationRepository], because this is
 * UI state that happens to be geographic — a screen's scroll position, in effect — while a
 * fix is a measurement of the world. A test for the map's restore behaviour should be able
 * to fake this without also faking a GPS.
 */
interface MapCameraRepository {

    /** The last saved viewport, or null if the map has never been opened. */
    suspend fun lastCamera(): MapCamera?

    /** Overwrites the saved viewport. Called when the map comes to rest, not while it moves. */
    suspend fun saveCamera(camera: MapCamera)
}
