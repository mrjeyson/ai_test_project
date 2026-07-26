package com.example.test_ai_project.feature.home.map

import androidx.annotation.StringRes
import com.example.test_ai_project.core.model.MapCamera
import com.example.test_ai_project.core.model.UserLocation

/**
 * Everything the Map tab renders.
 *
 * Flags rather than a sealed `Loading | Success | Error` hierarchy, for the same reason the
 * Movies tab uses them: the states are not mutually exclusive. The ordinary case here is a
 * *cached position* and *a fix in flight* and *the last attempt having failed*, all at once.
 */
data class MapUiState(
    val permission: LocationPermission = LocationPermission.Unknown,
    val isLocating: Boolean = false,
    val userLocation: UserLocation? = null,
    val startCamera: MapCamera? = null,
    val isReady: Boolean = false,
    @param:StringRes val messageRes: Int? = null,
) {
    val hasFix: Boolean get() = userLocation != null
    val isLocationLayerEnabled: Boolean get() = permission == LocationPermission.Granted
    val isShowingCachedOnly: Boolean
        get() = hasFix && permission == LocationPermission.Denied
}

enum class LocationPermission {
    Unknown,
    Granted,
    Denied,
}
