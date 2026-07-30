package com.example.test_ai_project.home.presentation.map.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.exception.LocationUnavailableException
import com.example.test_ai_project.home.domain.model.MapCamera
import com.example.test_ai_project.home.domain.service.LocationService
import com.example.test_ai_project.home.presentation.map.contract.MapEvent
import com.example.test_ai_project.home.presentation.map.contract.MapState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.base.BaseViewModel
import com.example.test_ai_project.resource.base.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.test_ai_project.home.presentation.map.contract.LocationPermission

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationService: LocationService,
) : BaseViewModel<MapState, MapEvent, NoEffect>(MapState()) {

    /**
     * The parts of the state this ViewModel decides, kept in one flow rather than five.
     *
     * `combine` tops out at five sources and the readability tops out well before that;
     * folding the cached position — the one genuinely external source — into a single
     * locally-owned state keeps this to two.
     */
    private val ownedState = MutableStateFlow(MapState())

    override val uiState: StateFlow<MapState> = combine(
        ownedState,
        locationService.observeLastKnownLocation(),
    ) { state, location ->
        state.copy(userLocation = location)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapState(),
        )

    override fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.PermissionResult -> onPermissionResult(event.isGranted)
            MapEvent.RefreshRequested -> refresh()
            is MapEvent.CameraSettled -> onCameraSettled(event.camera)
            MapEvent.MessageDismissed -> dismissMessage()
        }
    }

    private var locateJob: Job? = null

    /**
     * The last viewport written to disk.
     *
     * Panning produces a settle event every time the map comes to rest, and most of them
     * repeat the previous position — a tap that misses, a pinch that snaps back. Comparing
     * here keeps those off the disk.
     */
    private var savedCamera: MapCamera? = null

    init {
        viewModelScope.launch {
            val camera = locationService.lastCamera()
            savedCamera = camera
            ownedState.update { it.copy(startCamera = camera, isReady = true) }
        }
    }

    /**
     * Records the permission decision and, on a grant, immediately asks for a fix.
     *
     * Called both after the system dialog and when the screen finds the permission already
     * granted, so this is the single point where "we may locate the user" becomes true.
     */
    private fun onPermissionResult(isGranted: Boolean) {
        ownedState.update {
            it.copy(
                permission = if (isGranted) LocationPermission.Granted else LocationPermission.Denied,
                // A refusal is not an error banner: the map still works, and the screen
                // says so in its own words rather than as a failure message.
                messageRes = if (isGranted) it.messageRes else null,
            )
        }
        if (isGranted) refresh()
    }

    /** Explicit user action — the recentre control, or a retry after a failed attempt. */
    private fun refresh() {
        if (ownedState.value.permission != LocationPermission.Granted) return

        // Cancelling matters: tapping recentre twice while the first request waits on a GPS
        // lock would otherwise leave two attempts racing to clear the loading flag.
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            ownedState.update { it.copy(isLocating = true) }
            runCatching { locationService.refreshCurrentLocation() }
                .onSuccess { ownedState.update { state -> state.copy(messageRes = null) } }
                .onFailure { error ->
                    ownedState.update { state -> state.copy(messageRes = error.toMessageRes()) }
                }
            ownedState.update { it.copy(isLocating = false) }
        }
    }

    /**
     * The map came to rest at [camera].
     *
     * Saved on settle rather than on every frame of a pan: a drag emits hundreds of camera
     * positions, and only the one the user stopped on is worth persisting.
     */
    private fun onCameraSettled(camera: MapCamera) {
        if (camera == savedCamera) return
        savedCamera = camera
        viewModelScope.launch { locationService.saveCamera(camera) }
    }

    private fun dismissMessage() {
        ownedState.update { it.copy(messageRes = null) }
    }

    private fun Throwable.toMessageRes(): Int = when (this) {
        // A grant the user refused, discovered at request time rather than at the dialog —
        // revoked from Settings while the app was backgrounded, most likely.
        is LocationPermissionDeniedException -> ResR.string.map_error_permission
        is LocationUnavailableException -> ResR.string.map_error_unavailable
        else -> ResR.string.map_error_generic
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
