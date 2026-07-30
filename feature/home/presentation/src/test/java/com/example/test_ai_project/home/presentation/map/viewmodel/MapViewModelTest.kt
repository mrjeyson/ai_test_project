package com.example.test_ai_project.home.presentation.map.viewmodel

import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.exception.LocationUnavailableException
import com.example.test_ai_project.home.domain.model.MapCamera
import com.example.test_ai_project.home.domain.model.UserLocation
import com.example.test_ai_project.home.domain.service.LocationService
import com.example.test_ai_project.home.presentation.map.contract.LocationPermission
import com.example.test_ai_project.home.presentation.map.contract.MapEvent
import com.example.test_ai_project.home.presentation.map.contract.MapState
import com.example.test_ai_project.resource.R as ResR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Driven through [MapEvent]s, with the whole of [LocationService] faked — that interface is
 * the only thing this ViewModel can reach, so a fake of it is a complete substitute for
 * Room, Play services and the geocoder at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationService = FakeLocationService()

    private fun viewModel() = MapViewModel(locationService)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `is not ready until the saved camera has been read`() = runTest {
        locationService.camera = SavedCamera
        val viewModel = viewModel()
        observe(viewModel)

        assertFalse(viewModel.uiState.value.isReady)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(SavedCamera, viewModel.uiState.value.startCamera)
    }

    @Test
    fun `opens with no start camera when the map has never been used`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReady)
        assertNull(viewModel.uiState.value.startCamera)
    }

    @Test
    fun `a refused permission asks for no fix`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(MapEvent.PermissionResult(isGranted = false))
        advanceUntilIdle()

        assertEquals(LocationPermission.Denied, viewModel.uiState.value.permission)
        assertEquals(0, locationService.refreshCount)
        // A refusal is a state the screen explains in its own words, not an error banner.
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `a granted permission acquires a fix and publishes it`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(MapEvent.PermissionResult(isGranted = true))
        advanceUntilIdle()

        assertEquals(LocationPermission.Granted, viewModel.uiState.value.permission)
        assertEquals(1, locationService.refreshCount)
        assertEquals(FreshFix, viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `a failed fix keeps the cached position and says why`() = runTest {
        locationService.cached.value = CachedFix
        locationService.failure = LocationUnavailableException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(MapEvent.PermissionResult(isGranted = true))
        advanceUntilIdle()

        assertEquals(ResR.string.map_error_unavailable, viewModel.uiState.value.messageRes)
        // The whole point of the cache: the failure does not blank the map.
        assertEquals(CachedFix, viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `a permission revoked behind the app's back is reported as such`() = runTest {
        locationService.failure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(MapEvent.PermissionResult(isGranted = true))
        advanceUntilIdle()

        assertEquals(ResR.string.map_error_permission, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `recentring without the permission does nothing`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onEvent(MapEvent.PermissionResult(isGranted = false))
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.RefreshRequested)
        advanceUntilIdle()

        assertEquals(0, locationService.refreshCount)
    }

    @Test
    fun `settling twice on the same viewport writes once`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.CameraSettled(SavedCamera))
        viewModel.onEvent(MapEvent.CameraSettled(SavedCamera))
        viewModel.onEvent(MapEvent.CameraSettled(SavedCamera.copy(zoom = 17f)))
        advanceUntilIdle()

        assertEquals(
            listOf(SavedCamera, SavedCamera.copy(zoom = 17f)),
            locationService.saved,
        )
    }

    @Test
    fun `a viewport identical to the restored one is not rewritten`() = runTest {
        locationService.camera = SavedCamera
        val viewModel = viewModel()
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.CameraSettled(SavedCamera))
        advanceUntilIdle()

        assertTrue(locationService.saved.isEmpty())
    }

    @Test
    fun `dismissing clears the message`() = runTest {
        locationService.failure = LocationUnavailableException()
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onEvent(MapEvent.PermissionResult(isGranted = true))
        advanceUntilIdle()

        viewModel.onEvent(MapEvent.MessageDismissed)
        // The published state is a combine of two flows, so it lands a dispatch later than
        // the update that feeds it.
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.messageRes)
    }

    /**
     * `uiState` is shared [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so it
     * stays at its initial value until something collects it. Every test needs a
     * subscriber for the state to be live at all.
     */
    private fun TestScope.observe(viewModel: MapViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }
}

private val SavedCamera = MapCamera(latitude = 41.31, longitude = 69.24, zoom = 15f)

private val CachedFix = UserLocation(
    latitude = 41.31,
    longitude = 69.24,
    accuracyMeters = 40f,
    capturedAtEpochMillis = 1_753_000_000_000L,
)

private val FreshFix = UserLocation(
    latitude = 41.32,
    longitude = 69.25,
    accuracyMeters = 12f,
    capturedAtEpochMillis = 1_753_500_000_000L,
)

private class FakeLocationService : LocationService {
    val cached = MutableStateFlow<UserLocation?>(null)
    var failure: Throwable? = null
    var refreshCount = 0
    var camera: MapCamera? = null
    val saved = mutableListOf<MapCamera>()

    override fun observeLastKnownLocation(): Flow<UserLocation?> = cached

    override suspend fun refreshCurrentLocation() {
        refreshCount++
        failure?.let { throw it }
        cached.value = FreshFix
    }

    override suspend fun lastCamera(): MapCamera? = camera

    override suspend fun saveCamera(camera: MapCamera) {
        this.camera = camera
        saved += camera
    }
}
