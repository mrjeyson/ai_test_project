package com.example.test_ai_project.home.presentation.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Points `Dispatchers.Main` at a test dispatcher for the duration of a test.
 *
 * Every ViewModel test needs this, because `viewModelScope` is hard-wired to
 * `Dispatchers.Main` and there is no real main looper under a JVM unit test.
 *
 * [UnconfinedTestDispatcher] by default so that a coroutine launched by the code under
 * test runs eagerly to its first suspension point — which is what lets a test assert on
 * state immediately after calling a method, with no `advanceUntilIdle()`. The tabs that
 * fold in a ticking clock pass a `StandardTestDispatcher` instead, so the test can step the
 * scheduler itself rather than draining an infinite ticker.
 *
 * Lives in this module's own test source set. It is fifteen lines of JUnit boilerplate, and
 * a `:core:testing` module to share it would put a Gradle module on the graph that every
 * feature depends on to avoid typing them twice.
 */
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
