package com.example.test_ai_project.core.model

/**
 * Where the map is looking: a centre and a zoom level.
 *
 * Kept apart from [UserLocation] because the two answer different questions and change at
 * different times. The user's position is a fact the device reports; the camera is a view
 * the user chose by panning, and it survives being wrong about where they now are.
 *
 * Persisting it is what lets the tab reopen — cold, offline, days later — on the same
 * street the user left it on, instead of snapping back to a default.
 */
data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float,
) {
    companion object {
        /**
         * Street level: close enough that the user marker sits in recognisable
         * surroundings, wide enough to include the next junction.
         */
        const val DEFAULT_ZOOM = 15f

        /**
         * Used only before there is anything to show — no cached camera and no cached fix.
         * A continent-wide view rather than the null island the SDK would otherwise open
         * on, so the first frame reads as "not located yet" rather than "you are in the
         * Atlantic".
         */
        const val WORLD_ZOOM = 2f
    }
}
