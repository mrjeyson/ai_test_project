package com.example.test_ai_project.core.data.mapper

import android.location.Location
import com.example.test_ai_project.core.database.entity.LastKnownLocationEntity
import com.example.test_ai_project.core.database.entity.MapCameraEntity
import com.example.test_ai_project.core.model.MapCamera
import com.example.test_ai_project.core.model.UserLocation

/**
 * @param fallbackEpochMillis used when the platform reports no fix time. `Location.time` is
 *   normally the moment of the fix, but a synthetic or mocked provider can leave it at
 *   zero, and a fix stamped 1970 would be rendered as impossibly stale.
 */
internal fun Location.toEntity(fallbackEpochMillis: Long) = LastKnownLocationEntity(
    latitude = latitude,
    longitude = longitude,
    // hasAccuracy() distinguishes "no radius reported" from "a radius of zero metres";
    // Location.accuracy returns 0f for both.
    accuracyMeters = if (hasAccuracy()) accuracy else null,
    capturedAtEpochMillis = time.takeIf { it > 0L } ?: fallbackEpochMillis,
)

internal fun LastKnownLocationEntity.toDomain() = UserLocation(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    capturedAtEpochMillis = capturedAtEpochMillis,
)

internal fun MapCameraEntity.toDomain() = MapCamera(
    latitude = latitude,
    longitude = longitude,
    zoom = zoom,
)

internal fun MapCamera.toEntity() = MapCameraEntity(
    latitude = latitude,
    longitude = longitude,
    zoom = zoom,
)
