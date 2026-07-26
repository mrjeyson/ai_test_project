package com.example.test_ai_project.feature.home.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.model.MapCamera
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.core.ui.component.LoadingState
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.core.ui.theme.VaultTeal
import com.example.test_ai_project.feature.home.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the only place in this file that touches Hilt, the ViewModel, or
 * the permission API.
 */
@Composable
internal fun MapRoute(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        // Either grant is enough. Coarse alone still centres the map on the right
        // neighbourhood, and refusing to work without fine location would be a worse
        // answer than a slightly larger accuracy circle.
        onResult = { grants -> viewModel.onPermissionResult(grants.values.any { it }) },
    )

    LaunchedEffect(Unit) {
        when {
            // Already known to be granted — nothing to ask, nothing to re-request. This
            // runs again on every return to the tab, which is why it exits early rather
            // than firing a fresh fix request each visit.
            uiState.permission == LocationPermission.Granted -> Unit

            context.hasLocationPermission() -> viewModel.onPermissionResult(true)

            // Asked once, on the first visit. After a refusal the state is Denied and this
            // stops prompting — the screen offers a button instead, which is the only way
            // a second system dialog can appear.
            uiState.permission == LocationPermission.Unknown ->
                permissionLauncher.launch(LocationPermissions)

            else -> Unit
        }
    }

    MapScreen(
        uiState = uiState,
        onRecentre = viewModel::refresh,
        onCameraSettled = viewModel::onCameraSettled,
        onRequestPermission = { permissionLauncher.launch(LocationPermissions) },
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
internal fun MapScreen(
    uiState: MapUiState,
    onRecentre: () -> Unit,
    onCameraSettled: (MapCamera) -> Unit,
    onRequestPermission: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Held back until the saved camera has been read. Composing the map first would open it
    // on a default position and then jump, which reads as a glitch rather than as a
    // restore.
    if (!uiState.isReady) {
        LoadingState(modifier = modifier)
        return
    }

    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState { position = uiState.startPosition() }

    /**
     * Whether the map has been pointed at something deliberate.
     *
     * Initialised true when there is a restored camera — a viewport the user chose
     * themselves outranks a later fix, so the map stays where they left it and the
     * recentre control is how they come back.
     */
    var hasCentred by rememberSaveable {
        mutableStateOf(uiState.startCamera != null || uiState.userLocation != null)
    }

    // The first fix of a first run. Animated rather than moved, so the transition from the
    // world view reads as travel rather than a cut.
    LaunchedEffect(uiState.userLocation) {
        val location = uiState.userLocation
        if (hasCentred || location == null) return@LaunchedEffect
        hasCentred = true
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(location.toLatLng(), MapCamera.DEFAULT_ZOOM),
        )
    }

    cameraPositionState.PersistOnSettle(onCameraSettled = onCameraSettled)

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentDescription = stringResource(id = R.string.map_content_description),
            properties = MapProperties(isMyLocationEnabled = uiState.isLocationLayerEnabled),
            uiSettings = MapUiSettings(
                // Pan, zoom and pinch stay on — the whole point of a map. The SDK's own
                // my-location button is off because the app has its own, which works with
                // the cached fix and can also trigger a refresh.
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                zoomControlsEnabled = true,
            ),
        ) {
            uiState.userLocation?.let { location -> UserMarker(location = location) }
        }

        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            // A strip, not a spinner over the map: the cached position stays visible and
            // the map stays draggable while a fix is acquired.
            if (uiState.isLocating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (uiState.messageRes != null) {
                MapBanner(
                    messageRes = uiState.messageRes,
                    actionRes = R.string.map_retry,
                    onAction = onRecentre,
                    onDismiss = onDismissMessage,
                )
            } else if (uiState.permission == LocationPermission.Denied) {
                MapBanner(
                    messageRes = R.string.map_permission_body,
                    actionRes = R.string.map_permission_action,
                    onAction = onRequestPermission,
                    onDismiss = null,
                )
            }
        }

        if (uiState.isShowingCachedOnly) {
            CachedBadge(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp),
            )
        }

        FloatingActionButton(
            onClick = {
                onRecentre()
                // Moves to the cached position immediately rather than waiting on the fix:
                // offline, that is the only position there will ever be.
                uiState.userLocation?.let { location ->
                    hasCentred = true
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                location.toLatLng(),
                                MapCamera.DEFAULT_ZOOM,
                            ),
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VaultTeal,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            if (uiState.isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_my_location),
                    contentDescription = stringResource(id = R.string.map_recentre),
                )
            }
        }
    }
}

/**
 * The user's position: a pin, plus a circle for the reported accuracy.
 *
 * The circle is the honest part. A pin alone claims metre-precision the fix may not have,
 * and the difference matters most in exactly the case this screen is built for — a coarse,
 * cached, offline position.
 */
@Composable
private fun UserMarker(location: UserLocation) {
    val position = location.toLatLng()

    location.accuracyMeters?.let { accuracy ->
        Circle(
            center = position,
            radius = accuracy.toDouble(),
            fillColor = VaultTeal.copy(alpha = 0.12f),
            strokeColor = VaultTeal.copy(alpha = 0.45f),
            strokeWidth = 2f,
        )
    }

    Marker(
        state = rememberUpdatedMarkerState(position = position),
        title = stringResource(id = R.string.map_marker_title),
        snippet = location.accuracyMeters?.let { accuracy ->
            stringResource(id = R.string.map_marker_accuracy, accuracy.toInt())
        },
    )
}

/**
 * Saves the viewport once the map comes to rest.
 *
 * Two filters, both load-bearing. Only settled positions are saved, because a drag emits a
 * new camera every frame. And only user-caused movement is saved — [CameraMoveStartedReason
 * .NO_MOVEMENT_YET] would otherwise persist the placeholder world view on a first run, and
 * that saved camera would then suppress the auto-centre on every run after it.
 */
@Composable
private fun CameraPositionState.PersistOnSettle(onCameraSettled: (MapCamera) -> Unit) {
    LaunchedEffect(isMoving) {
        if (isMoving) return@LaunchedEffect
        val isUserCaused = cameraMoveStartedReason == CameraMoveStartedReason.GESTURE ||
            cameraMoveStartedReason == CameraMoveStartedReason.DEVELOPER_ANIMATION
        if (!isUserCaused) return@LaunchedEffect

        onCameraSettled(
            MapCamera(
                latitude = position.target.latitude,
                longitude = position.target.longitude,
                zoom = position.zoom,
            ),
        )
    }
}

/**
 * Advisory, not blocking. Whatever it reports, the map underneath it still works and the
 * cached position is still on it — so this floats above the map rather than replacing it.
 */
@Composable
private fun MapBanner(
    messageRes: Int,
    actionRes: Int,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(text = stringResource(id = actionRes))
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.map_dismiss))
                }
            }
        }
    }
}

/** Says out loud that the marker is a remembered position, not a live one. */
@Composable
private fun CachedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = R.string.map_cached_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Where the map opens, in priority order: the viewport the user left it on, then the last
 * cached fix, then a world view.
 *
 * The world view exists so a first run with no permission opens on something legible rather
 * than at zoom 15 on the null island in the Gulf of Guinea.
 */
private fun MapUiState.startPosition(): CameraPosition {
    val camera = startCamera
    val location = userLocation
    return when {
        camera != null -> CameraPosition.fromLatLngZoom(
            LatLng(camera.latitude, camera.longitude),
            camera.zoom,
        )

        location != null -> CameraPosition.fromLatLngZoom(
            location.toLatLng(),
            MapCamera.DEFAULT_ZOOM,
        )

        else -> CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), MapCamera.WORLD_ZOOM)
    }
}

private fun UserLocation.toLatLng() = LatLng(latitude, longitude)

private fun Context.hasLocationPermission(): Boolean = LocationPermissions.any { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)

// The map itself renders as an empty box in a preview host — the SDK needs a real
// MapView — so these previews are of the furniture around it, which is the part that
// changes with state.

@Preview(showBackground = true)
@Composable
private fun MapScreenLocatingPreview() {
    AppTheme {
        MapScreen(
            uiState = MapUiState(
                permission = LocationPermission.Granted,
                isLocating = true,
                userLocation = previewLocation(),
                isReady = true,
            ),
            onRecentre = {},
            onCameraSettled = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenDeniedPreview() {
    AppTheme {
        MapScreen(
            uiState = MapUiState(
                permission = LocationPermission.Denied,
                userLocation = previewLocation(),
                isReady = true,
            ),
            onRecentre = {},
            onCameraSettled = {},
            onRequestPermission = {},
            onDismissMessage = {},
        )
    }
}

private fun previewLocation() = UserLocation(
    latitude = 41.311081,
    longitude = 69.240562,
    accuracyMeters = 18f,
    capturedAtEpochMillis = 1_753_500_000_000L,
)
