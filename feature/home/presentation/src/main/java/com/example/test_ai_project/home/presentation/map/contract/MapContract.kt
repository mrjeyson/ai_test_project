package com.example.test_ai_project.home.presentation.map.contract

import androidx.annotation.StringRes
import com.example.test_ai_project.home.domain.model.MapCamera
import com.example.test_ai_project.home.domain.model.UserLocation
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the Map tab renders.
 *
 * Flags rather than a sealed `Loading | Success | Error` hierarchy, for the same reason the
 * Movies tab uses them: the states are not mutually exclusive. The ordinary case here is a
 * *cached position* and *a fix in flight* and *the last attempt having failed*, all at once.
 */
data class MapState(
    val permission: LocationPermission = LocationPermission.Unknown,
    val isLocating: Boolean = false,
    val userLocation: UserLocation? = null,
    val startCamera: MapCamera? = null,
    val isReady: Boolean = false,
    @param:StringRes val messageRes: Int? = null,
) : UiState {
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

sealed interface MapEvent : UiEvent {
    /** The location permission dialog was answered. */
    data class PermissionResult(val isGranted: Boolean) : MapEvent

    /** Re-detect the device position. */
    data object RefreshRequested : MapEvent

    /**
     * The user stopped panning or zooming.
     *
     * Reported on settle rather than on every frame: persisting each intermediate camera
     * would write to Room dozens of times per gesture.
     */
    data class CameraSettled(val camera: MapCamera) : MapEvent

    data object MessageDismissed : MapEvent
}
