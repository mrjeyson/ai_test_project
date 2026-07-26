package com.example.test_ai_project.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The most recent position fix, kept so the map has something to draw on a cold, offline
 * start.
 *
 * A single row pinned to [SINGLETON_ID], not a history: nothing in the app asks where the
 * user was an hour ago, and a growing table of fixes would be a location trail — a thing
 * worth not storing at all rather than storing carefully.
 */
@Entity(tableName = "last_known_location")
data class LastKnownLocationEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

/**
 * Where the map was last left pointing.
 *
 * Separate table from [LastKnownLocationEntity] for the same reason the domain keeps the
 * two models apart: panning away from yourself is a normal thing to do, and one write must
 * not clobber the other.
 */
@Entity(tableName = "map_camera")
data class MapCameraEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val zoom: Float,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
