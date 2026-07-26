package com.example.test_ai_project.feature.splash

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * `viewModelScope` runs on `Dispatchers.Main`, so the test swaps in a test dispatcher and
 * lets `runTest` skip the per-stage delays — the sequence is asserted, the wall clock is
 * not waited on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on the first stage with no progress`() = runTest {
        val viewModel = SplashViewModel()

        assertEquals(
            SplashUiState.Initializing(BootstrapStage.LocalEnvironment, progress = 0f),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `walks every stage in declaration order and ends ready`() = runTest {
        val viewModel = SplashViewModel()

        viewModel.uiState.test {
            val stages = mutableListOf<BootstrapStage>()
            var state = awaitItem()
            while (state is SplashUiState.Initializing) {
                if (stages.lastOrNull() != state.stage) stages += state.stage
                state = awaitItem()
            }

            assertEquals(BootstrapStage.entries.toList(), stages)
            assertEquals(SplashUiState.Ready, state)
        }
    }

    @Test
    fun `progress is monotonic and reaches one before ready`() = runTest {
        val viewModel = SplashViewModel()

        viewModel.uiState.test {
            var previous = -1f
            var state = awaitItem()
            while (state is SplashUiState.Initializing) {
                assertTrue(
                    "progress went backwards: $previous -> ${state.progress}",
                    state.progress >= previous,
                )
                previous = state.progress
                state = awaitItem()
            }

            assertEquals(1f, previous, 0f)
            assertEquals(SplashUiState.Ready, state)
        }
    }
}
