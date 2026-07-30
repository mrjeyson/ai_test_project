package com.example.test_ai_project.home.presentation.weather.viewmodel

import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.exception.WeatherKeyRejectedException
import com.example.test_ai_project.home.domain.exception.WeatherServiceNotConfiguredException
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.CurrentWeather
import com.example.test_ai_project.home.domain.model.DailyForecast
import com.example.test_ai_project.home.domain.model.HourlyForecast
import com.example.test_ai_project.home.domain.model.UserLocation
import com.example.test_ai_project.home.domain.model.WeatherCondition
import com.example.test_ai_project.home.domain.model.WeatherSnapshot
import com.example.test_ai_project.home.domain.service.WeatherService
import com.example.test_ai_project.home.presentation.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.test_ai_project.home.presentation.weather.contract.DailyRow
import com.example.test_ai_project.home.presentation.weather.contract.HourlyColumn
import com.example.test_ai_project.home.presentation.weather.contract.WeatherEvent
import com.example.test_ai_project.home.presentation.weather.contract.WeatherState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.home.presentation.testing.MainDispatcherRule

/**
 * The real use cases are used rather than fakes — they are thin orchestration over the
 * repositories, and doubling them would mean testing the ViewModel against a version of the rules
 * written for the test. The repositories, which are the parts with I/O, are what gets faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val weatherService = FakeWeatherService()
    private val clock = MutableTimeProvider()

    private fun viewModel() = WeatherViewModel(
        weatherService = weatherService,
        timeProvider = clock,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clock.now = Now
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the hourly strip leads with a now column built from current conditions`() = runTest {
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val hourly = viewModel.uiState.value.hourly
        assertTrue(hourly.first().isNow)
        // The provider's window starts at the *next* step, so without a synthesised leading column
        // the strip would open up to three hours ahead of where the user is standing.
        assertEquals(-2.0, hourly.first().temperatureCelsius, 0.001)
        assertEquals(Now, hourly.first().startEpochMillis)
        assertFalse(hourly.drop(1).any { it.isNow })
    }

    @Test
    fun `the hourly strip stops at 24 hours and drops steps already past`() = runTest {
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        // The fixture spans -3h to +30h in three-hour steps. Only the eight inside the next 24
        // hours survive, plus the "now" column.
        val forecastColumns = viewModel.uiState.value.hourly.drop(1)
        assertEquals(8, forecastColumns.size)
        assertTrue(forecastColumns.all { it.startEpochMillis > Now })
        assertTrue(forecastColumns.all { it.startEpochMillis <= Now + hours(24) })
    }

    @Test
    fun `the daily bars are scaled to the whole list, not to each row`() = runTest {
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val rows = viewModel.uiState.value.daily
        // The fixture spans -6°..6°, so the coldest low anchors at 0 and the warmest high at 1.
        assertEquals(0f, rows.minOf { it.barStartFraction }, 0.001f)
        assertEquals(1f, rows.maxOf { it.barEndFraction }, 0.001f)
        // A per-row scale would make every bar full width and carry no information at all.
        assertTrue(rows.first().barEndFraction < 1f)
    }

    @Test
    fun `a flat forecast does not put NaN in the bars`() = runTest {
        // A real forecast for a maritime winter: the same degree every day. Dividing by the span
        // without a guard would make every fraction NaN and every bar vanish.
        weatherService.cached.value = snapshot(
            daily = List(4) { index ->
                DailyForecast(
                    date = CalendarDate(2026, 7, 26 + index),
                    highCelsius = 3.0,
                    lowCelsius = 3.0,
                    condition = WeatherCondition.Clouds,
                )
            },
        )
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertTrue(
            viewModel.uiState.value.daily.all {
                !it.barStartFraction.isNaN() && !it.barEndFraction.isNaN()
            },
        )
    }

    @Test
    fun `only the first day is marked today`() = runTest {
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val rows = viewModel.uiState.value.daily
        assertTrue(rows.first().isToday)
        assertFalse(rows.drop(1).any { it.isToday })
    }

    @Test
    fun `no row is today when the forecast window has already rolled past midnight`() = runTest {
        // 23:50 local. The provider's window opens at the next three-hour step, so the first row it
        // returns is tomorrow's — and there is no row for today at all.
        clock.now = seconds(MidnightUtc + 23 * 3600 + 50 * 60)
        weatherService.cached.value = snapshot(
            daily = listOf(
                DailyForecast(CalendarDate(2026, 7, 27), highCelsius = 2.0, lowCelsius = -3.0, condition = WeatherCondition.Clouds),
                DailyForecast(CalendarDate(2026, 7, 28), highCelsius = 4.0, lowCelsius = 0.0, condition = WeatherCondition.Clear),
            ),
        )
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        // Taking "today" to be the first row would label tomorrow "Today" on the one evening it
        // matters, and the label is the only thing anchoring the list to a date.
        assertFalse(viewModel.uiState.value.daily.any { it.isToday })
    }

    @Test
    fun `the today label follows the location's clock, not the device's`() = runTest {
        // 01:00 UTC, at UTC-5 — so it is still the previous evening where the readings belong.
        clock.now = seconds(MidnightUtc + 25 * 3600)
        weatherService.cached.value = snapshot(
            zoneOffsetSeconds = -5 * 3600,
            daily = listOf(
                DailyForecast(CalendarDate(2026, 7, 26), highCelsius = 1.0, lowCelsius = -6.0, condition = WeatherCondition.Snow),
                DailyForecast(CalendarDate(2026, 7, 27), highCelsius = 2.0, lowCelsius = -3.0, condition = WeatherCondition.Clouds),
            ),
        )
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val today = viewModel.uiState.value.daily.single { it.isToday }
        // Against UTC this instant is the 27th. Where the reading was taken it is still the 26th.
        assertEquals(CalendarDate(2026, 7, 26), today.date)
    }

    @Test
    fun `days already past are dropped from the daily list`() = runTest {
        // A week offline. Every cached day is now behind the clock except the last two.
        clock.now = seconds(MidnightUtc + 48 * 3600)
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val rows = viewModel.uiState.value.daily
        // Rendered as bare weekday names, a past "Mon" reads as the Monday coming.
        assertEquals(
            listOf(CalendarDate(2026, 7, 28), CalendarDate(2026, 7, 29)),
            rows.map { it.date },
        )
        // And the track is rescaled over what survived, or every visible bar would sit bunched in
        // the middle of a range set by days no longer on screen.
        assertEquals(0f, rows.minOf { it.barStartFraction }, 0.001f)
        assertEquals(1f, rows.maxOf { it.barEndFraction }, 0.001f)
    }

    @Test
    fun `a cache older than the whole forecast window renders no daily list`() = runTest {
        clock.now = seconds(MidnightUtc + 30 * 24 * 3600)
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertTrue(viewModel.uiState.value.daily.isEmpty())
        assertTrue(viewModel.uiState.value.hourly.isEmpty())
        // The reading itself is still shown — stale, and labelled so.
        assertTrue(viewModel.uiState.value.isCached)
        assertTrue(viewModel.uiState.value.isStale)
    }

    @Test
    fun `a fresh reading is not stale and an old one is`() = runTest {
        weatherService.cached.value = snapshot()
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertFalse(viewModel.uiState.value.isStale)

        // Two hours later, with nothing refetched — the page has been open the whole time.
        clock.now = Now + hours(2)
        tick()

        // Left unflagged, an hours-old temperature would be presented as the present one, which is
        // the one thing a weather page must not do.
        assertTrue(viewModel.uiState.value.isStale)
    }

    @Test
    fun `a failed fetch keeps the cached reading and says why`() = runTest {
        weatherService.cached.value = snapshot()
        weatherService.failure = IOException("offline")
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        assertEquals(ResR.string.weather_error_unreachable, viewModel.uiState.value.messageRes)
        // The whole point of the cache: the failure does not blank the page.
        assertTrue(viewModel.uiState.value.isCached)
        assertFalse(viewModel.uiState.value.isLoading)
        // Nothing to offer a grant for — this is a network problem, not a permission one.
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a missing API key is reported as a build fault, not a network one`() = runTest {
        weatherService.cached.value = snapshot()
        weatherService.failure = WeatherServiceNotConfiguredException("no key")
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        // "You are offline, try again" would send the user retrying forever against a problem no
        // number of retries can fix.
        assertEquals(ResR.string.weather_error_not_configured, viewModel.uiState.value.messageRes)
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a rejected key is told apart from an absent one`() = runTest {
        weatherService.cached.value = snapshot()
        weatherService.failure = WeatherKeyRejectedException("401")
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        // Telling someone to add a key they can plainly see in their own local.properties reads as
        // the app being broken. The two faults share a shape and not a remedy.
        assertEquals(ResR.string.weather_error_key_rejected, viewModel.uiState.value.messageRes)
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refused location permission offers the grant instead of a retry`() = runTest {
        weatherService.cached.value = null
        weatherService.hasCachedFix = false
        weatherService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        assertEquals(ResR.string.weather_error_permission, viewModel.uiState.value.messageRes)
        assertTrue(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refusal still works from a fix another tab already cached`() = runTest {
        // No permission this session, but coordinates are already on disk — so the reading can be
        // fetched without ever asking the platform for a new fix.
        weatherService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        assertEquals(0, weatherService.relocateCount)
        assertEquals(1, weatherService.refreshCount)
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `refresh re-detects the location and bypasses the freshness window`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.RefreshRequested)
        settle()

        assertEquals(1, weatherService.relocateCount)
        // Without this the button would look broken: the repository would decline to refetch a
        // nine-minute-old snapshot that the user has just told us is not good enough.
        assertTrue(weatherService.lastForce)
    }

    @Test
    fun `an ordinary load reuses the cached fix rather than acquiring one`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(WeatherEvent.RetryRequested)
        settle()

        assertEquals(0, weatherService.relocateCount)
        assertFalse(weatherService.lastForce)
    }

    @Test
    fun `dismissing clears the message and the grant offer together`() = runTest {
        weatherService.cached.value = null
        weatherService.hasCachedFix = false
        weatherService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onEvent(WeatherEvent.PermissionsResolved)
        settle()

        viewModel.onEvent(WeatherEvent.MessageDismissed)
        settle()

        assertNull(viewModel.uiState.value.messageRes)
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `falls back to coordinates when the place has no name`() = runTest {
        weatherService.cached.value = snapshot().copy(placeName = null)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertEquals("64.1466, -21.9426", viewModel.uiState.value.place)
    }

    @Test
    fun `an empty cache is empty rather than loading`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isCached)
        assertFalse(viewModel.uiState.value.isInitialLoad)
    }

    private fun snapshot(
        daily: List<DailyForecast> = defaultDaily(),
        zoneOffsetSeconds: Int = 0,
    ) = WeatherSnapshot(
        latitude = 64.1466,
        longitude = -21.9426,
        placeName = "Reykjavík, IS",
        zoneOffsetSeconds = zoneOffsetSeconds,
        current = CurrentWeather(
            temperatureCelsius = -2.0,
            feelsLikeCelsius = -8.0,
            highCelsius = 1.0,
            lowCelsius = -5.0,
            condition = WeatherCondition.Clouds,
            description = "broken clouds",
            isNight = false,
            windMetresPerSecond = 3.9,
            humidityPercent = 78,
            visibilityMetres = 12_000,
        ),
        // Deliberately starts in the past and runs well past 24 hours, so the window filter has
        // something to cut at both ends.
        hourly = (-1..10).map { step ->
            HourlyForecast(
                startEpochMillis = Now + step * hours(3),
                temperatureCelsius = -2.0 + step,
                condition = WeatherCondition.Snow,
                isNight = false,
            )
        },
        daily = daily,
        fetchedAtEpochMillis = Now - 4 * 60 * 1_000L,
    )

    private fun defaultDaily() = listOf(
        DailyForecast(CalendarDate(2026, 7, 26), highCelsius = 1.0, lowCelsius = -6.0, condition = WeatherCondition.Snow),
        DailyForecast(CalendarDate(2026, 7, 27), highCelsius = 2.0, lowCelsius = -3.0, condition = WeatherCondition.Clouds),
        DailyForecast(CalendarDate(2026, 7, 28), highCelsius = 4.0, lowCelsius = 0.0, condition = WeatherCondition.Clear),
        DailyForecast(CalendarDate(2026, 7, 29), highCelsius = 6.0, lowCelsius = 2.0, condition = WeatherCondition.Clear),
    )

    /**
     * Runs everything already scheduled, without moving the clock.
     *
     * Deliberately not `advanceUntilIdle`, which would never return: the ViewModel's ticker is an
     * unbounded `while (true) { emit(); delay() }`, so there is always one more task pending and
     * "until idle" is a state this graph never reaches.
     */
    private fun TestScope.settle() = runCurrent()

    /** Moves the clock on by one tick of the ViewModel's ticker, then settles. */
    private fun TestScope.tick() {
        advanceTimeBy(TickIntervalMillis)
        runCurrent()
    }

    /**
     * `uiState` and the internal ticker are shared `WhileSubscribed`, so they stay at their initial
     * values until something collects them. Every test needs a subscriber for the state to be live.
     */
    private fun TestScope.observe(viewModel: WeatherViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private companion object {
        /** 2026-07-26T00:00:00Z, in epoch seconds — the anchor the fixtures are offset from. */
        const val MidnightUtc = 1_785_024_000L

        /** 26 July 2026, 13:28 UTC. */
        const val Now = 1_785_072_480_000L

        fun seconds(value: Long): Long = value * 1_000L

        /** Mirrors the ViewModel's own tick interval, which is private to it. */
        const val TickIntervalMillis = 15 * 60 * 1_000L

        val Fix = UserLocation(
            latitude = 64.1466,
            longitude = -21.9426,
            accuracyMeters = 30f,
            capturedAtEpochMillis = Now,
        )

        fun hours(count: Int): Long = count * 3_600_000L
    }
}

private class FakeWeatherService : WeatherService {
    val cached = MutableStateFlow<WeatherSnapshot?>(null)
    var failure: Throwable? = null
    var refreshCount = 0
    var relocateCount = 0
    var hasCachedFix = true
    var locationFailure: Throwable? = null
    var lastForce = false

    override fun observeWeather(): Flow<WeatherSnapshot?> = cached

    /**
     * Models the real service's two failure modes separately, because the ViewModel treats
     * them differently. [locationFailure] only surfaces when a fix was actually needed —
     * a caller with coordinates already on disk never asks the platform, which is what
     * makes "a refusal still works from a cached fix" assertable. [failure] is the fetch
     * itself going wrong, and surfaces either way.
     */
    override suspend fun refresh(relocate: Boolean) {
        refreshCount++
        if (relocate || !hasCachedFix) {
            relocateCount++
            locationFailure?.let { throw it }
        }
        lastForce = relocate
        failure?.let { throw it }
    }
}


private class MutableTimeProvider : TimeProvider {
    var now: Long = 0L
    override fun nowEpochMillis(): Long = now
}
