package com.example.test_ai_project.feature.home.map

import com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.core.domain.exception.LocationUnavailableException
import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.domain.repository.MapCameraRepository
import com.example.test_ai_project.core.domain.usecase.GetLastKnownLocationUseCase
import com.example.test_ai_project.core.domain.usecase.GetSavedMapCameraUseCase
import com.example.test_ai_project.core.domain.usecase.RefreshCurrentLocationUseCase
import com.example.test_ai_project.core.domain.usecase.SaveMapCameraUseCase
import com.example.test_ai_project.core.model.MapCamera
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.feature.home.R
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
 * The real use cases are used rather than fakes — they are one-line delegations, and
 * doubling them would only test that the double agrees with itself. The repositories, which
 * are the parts with I/O, are what gets faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val locationRepository = FakeLocationRepository()
    private val cameraRepository = FakeMapCameraRepository()

    private fun viewModel() = MapViewModel(
        getLastKnownLocation = GetLastKnownLocationUseCase(locationRepository),
        refreshCurrentLocation = RefreshCurrentLocationUseCase(locationRepository),
        getSavedMapCamera = GetSavedMapCameraUseCase(cameraRepository),
        saveMapCamera = SaveMapCameraUseCase(cameraRepository),
    )

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
        cameraRepository.camera = SavedCamera
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

        viewModel.onPermissionResult(isGranted = false)
        advanceUntilIdle()

        assertEquals(LocationPermission.Denied, viewModel.uiState.value.permission)
        assertEquals(0, locationRepository.refreshCount)
        // A refusal is a state the screen explains in its own words, not an error banner.
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `a granted permission acquires a fix and publishes it`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        assertEquals(LocationPermission.Granted, viewModel.uiState.value.permission)
        assertEquals(1, locationRepository.refreshCount)
        assertEquals(FreshFix, viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `a failed fix keeps the cached position and says why`() = runTest {
        locationRepository.cached.value = CachedFix
        locationRepository.failure = LocationUnavailableException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        assertEquals(R.string.map_error_unavailable, viewModel.uiState.value.messageRes)
        // The whole point of the cache: the failure does not blank the map.
        assertEquals(CachedFix, viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `a permission revoked behind the app's back is reported as such`() = runTest {
        locationRepository.failure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        assertEquals(R.string.map_error_permission, viewModel.uiState.value.messageRes)
    }

    @Test
    fun `recentring without the permission does nothing`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onPermissionResult(isGranted = false)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(0, locationRepository.refreshCount)
    }

    @Test
    fun `settling twice on the same viewport writes once`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCameraSettled(SavedCamera)
        viewModel.onCameraSettled(SavedCamera)
        viewModel.onCameraSettled(SavedCamera.copy(zoom = 17f))
        advanceUntilIdle()

        assertEquals(
            listOf(SavedCamera, SavedCamera.copy(zoom = 17f)),
            cameraRepository.saved,
        )
    }

    @Test
    fun `a viewport identical to the restored one is not rewritten`() = runTest {
        cameraRepository.camera = SavedCamera
        val viewModel = viewModel()
        observe(viewModel)
        advanceUntilIdle()

        viewModel.onCameraSettled(SavedCamera)
        advanceUntilIdle()

        assertTrue(cameraRepository.saved.isEmpty())
    }

    @Test
    fun `dismissing clears the message`() = runTest {
        locationRepository.failure = LocationUnavailableException()
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        viewModel.dismissMessage()
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

private class FakeLocationRepository : LocationRepository {
    val cached = MutableStateFlow<UserLocation?>(null)
    var failure: Throwable? = null
    var refreshCount = 0

    override fun observeLastKnownLocation(): Flow<UserLocation?> = cached

    override suspend fun refreshCurrentLocation() {
        refreshCount++
        failure?.let { throw it }
        cached.value = FreshFix
    }
}

private class FakeMapCameraRepository : MapCameraRepository {
    var camera: MapCamera? = null
    val saved = mutableListOf<MapCamera>()

    override suspend fun lastCamera(): MapCamera? = camera

    override suspend fun saveCamera(camera: MapCamera) {
        this.camera = camera
        saved += camera
    }
}
