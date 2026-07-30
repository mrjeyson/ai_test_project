package com.example.test_ai_project.home.presentation.prayertimes.viewmodel

import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.service.DateProvider
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.Prayer
import com.example.test_ai_project.home.domain.model.PrayerDay
import com.example.test_ai_project.home.domain.model.PrayerSchedule
import com.example.test_ai_project.home.domain.service.PrayerService
import com.example.test_ai_project.home.domain.model.PrayerTime
import com.example.test_ai_project.home.domain.model.UserLocation
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
import kotlinx.coroutines.test.runCurrent
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
import com.example.test_ai_project.home.presentation.prayertimes.contract.NextPrayer
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerEntry
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerStatus
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesEvent
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.home.presentation.testing.MainDispatcherRule

/**
 * The real use cases are used rather than fakes — they are thin orchestration over the
 * repositories, and doubling them would mean testing the ViewModel against a version of the
 * rules written for the test. The repositories and the alarm scheduler, which are the parts
 * with I/O, are what gets faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prayerService = FakePrayerService()
    private val clock = MutableTimeProvider()

    private val dateProvider = MutableDateProvider(Today)

    private fun viewModel(): PrayerTimesViewModel {
        return PrayerTimesViewModel(
            prayerService = prayerService,
            dateProvider = dateProvider,
            timeProvider = clock,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `marks past prayers completed, the soonest next, and the rest later`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertEquals(
            listOf(
                PrayerStatus.Completed,
                PrayerStatus.Completed,
                PrayerStatus.Next,
                PrayerStatus.Later,
                PrayerStatus.Later,
            ),
            viewModel.uiState.value.entries.map(PrayerEntry::status),
        )
        assertEquals(Prayer.Asr, viewModel.uiState.value.next?.prayer)
        assertFalse(viewModel.uiState.value.next!!.isTomorrow)
    }

    @Test
    fun `after Isha the next prayer is tomorrow's Fajr and no row is highlighted`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(22)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        val state = viewModel.uiState.value
        assertEquals(Prayer.Fajr, state.next?.prayer)
        assertTrue(state.next!!.isTomorrow)
        // The subtle one: today's Fajr shares a name with the next prayer but is fifteen
        // hours in the past, and highlighting it would point the user at the wrong row.
        assertTrue(state.entries.all { it.status == PrayerStatus.Completed })
    }

    @Test
    fun `the countdown tracks the next prayer and ticks down`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertEquals(hours(2), viewModel.remainingMillis.value)

        clock.now = TodayStart + hours(13) + 60_000L
        tick()

        assertEquals(hours(2) - 60_000L, viewModel.remainingMillis.value)
    }

    @Test
    fun `a failed fetch keeps the cached timetable and says why`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        prayerService.failure = IOException("offline")
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        assertEquals(ResR.string.prayer_error_unreachable, viewModel.uiState.value.messageRes)
        // The whole point of the cache: the failure does not blank the page.
        assertEquals(5, viewModel.uiState.value.entries.size)
        assertFalse(viewModel.uiState.value.isLoading)
        // Nothing to offer a grant for — this is a network problem, not a permission one.
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refused location permission offers the grant instead of a retry`() = runTest {
        prayerService.cached.value = null
        prayerService.hasCachedFix = false
        prayerService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        assertEquals(ResR.string.prayer_error_permission, viewModel.uiState.value.messageRes)
        assertTrue(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refusal still works from a fix the map already cached`() = runTest {
        // No permission this session, but coordinates are already on disk — so the
        // timetable can be fetched without ever asking the platform for a new fix.
        prayerService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        assertEquals(0, prayerService.relocateCount)
        assertEquals(1, prayerService.refreshCount)
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `CHANGE re-detects the location and bypasses the freshness check`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.LocationChangeRequested)
        settle()

        assertEquals(1, prayerService.relocateCount)
        assertTrue(prayerService.lastForce)
    }

    @Test
    fun `an ordinary load reuses the cached fix rather than acquiring one`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.RetryRequested)
        settle()

        assertEquals(0, prayerService.relocateCount)
        assertFalse(prayerService.lastForce)
    }

    @Test
    fun `a successful refresh re-derives the alarms`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        // Which alarms get derived is PrayerService's business and is asserted against the
        // service itself; all this layer can see is that a successful refresh re-armed them.
        assertEquals(1, prayerService.scheduleAlertsCount)
    }

    @Test
    fun `a failed refresh leaves the existing alarms alone`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        prayerService.failure = IOException("offline")
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        // Rewriting them from a fetch that failed could only make them worse: the alarms
        // already pending were derived from the same cache that is still on screen.
        assertEquals(0, prayerService.scheduleAlertsCount)
    }

    @Test
    fun `dismissing clears the message and the grant offer together`() = runTest {
        prayerService.cached.value = null
        prayerService.hasCachedFix = false
        prayerService.locationFailure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onEvent(PrayerTimesEvent.PermissionsResolved)
        settle()

        viewModel.onEvent(PrayerTimesEvent.MessageDismissed)
        settle()

        assertNull(viewModel.uiState.value.messageRes)
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `crossing midnight switches the page to the new day`() = runTest {
        prayerService.byDate[Today] = twoDaySchedule()
        prayerService.byDate[Tomorrow] =
            PrayerSchedule(today = day(TodayStart + hours(24)), tomorrow = null)
        clock.now = TodayStart + hours(23)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertEquals(TodayStart + hours(4), viewModel.uiState.value.entries.first().startEpochMillis)

        // 00:01. The page has been open the whole time and nothing has been refetched.
        dateProvider.date = Tomorrow
        clock.now = TodayStart + hours(24) + 60_000L
        tick()

        // Left unfixed, this still shows yesterday — every prayer completed, counting down
        // to nothing — on the one morning the user most needs it to be right.
        assertEquals(
            TodayStart + hours(28),
            viewModel.uiState.value.entries.first().startEpochMillis,
        )
        assertEquals(listOf(Today, Tomorrow), prayerService.observedDates)
    }

    @Test
    fun `a tick that does not cross midnight does not requery`() = runTest {
        prayerService.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        repeat(5) { tick() }

        // One query, not six: the date is distinct-until-changed, so a second passing costs
        // nothing at the database.
        assertEquals(listOf(Today), prayerService.observedDates)
    }

    @Test
    fun `falls back to coordinates when the place has no name`() = runTest {
        prayerService.cached.value = PrayerSchedule(
            today = day(TodayStart).copy(locationLabel = null),
            tomorrow = null,
        )
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        assertEquals("51.5072, -0.1276", viewModel.uiState.value.locationLabel)
    }

    private fun twoDaySchedule() = PrayerSchedule(
        today = day(TodayStart),
        tomorrow = day(TodayStart + hours(24)),
    )

    private fun day(startOfDay: Long) = PrayerDay(
        date = Today,
        latitude = 51.5072,
        longitude = -0.1276,
        zoneId = "Europe/London",
        locationLabel = "London, United Kingdom",
        times = listOf(
            PrayerTime(Prayer.Fajr, startOfDay + hours(4)),
            PrayerTime(Prayer.Dhuhr, startOfDay + hours(12)),
            PrayerTime(Prayer.Asr, startOfDay + hours(15)),
            PrayerTime(Prayer.Maghrib, startOfDay + hours(18)),
            PrayerTime(Prayer.Isha, startOfDay + hours(20)),
        ),
        fetchedAtEpochMillis = startOfDay,
    )

    /**
     * `uiState`, `remainingMillis` and the internal ticker are all shared
     * [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so they stay at their
     * initial values until something collects them. Every test needs a subscriber for the
     * state to be live at all.
     */
    /**
     * Runs everything already scheduled, without moving the clock.
     *
     * Deliberately not `advanceUntilIdle`, which would never return: the ViewModel's ticker
     * is an unbounded `while (true) { emit(); delay(1s) }`, so there is always one more task
     * pending and "until idle" is a state this graph never reaches. Nothing else on the
     * screen delays at all, so running the current instant settles all of it.
     */
    private fun TestScope.settle() = runCurrent()

    /** Moves the clock on by one tick of the ViewModel's ticker, then settles. */
    private fun TestScope.tick() {
        advanceTimeBy(TickIntervalMillis)
        runCurrent()
    }

    private fun TestScope.observe(viewModel: PrayerTimesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.remainingMillis.collect {}
        }
    }

    private companion object {
        val Today = CalendarDate(2026, 7, 26)
        val Tomorrow = CalendarDate(2026, 7, 27)

        /** Local midnight, 26 July 2026, in London. */
        const val TodayStart = 1_785_020_400_000L

        /** Mirrors the ViewModel's own tick interval, which is private to it. */
        const val TickIntervalMillis = 1_000L

        val Fix = UserLocation(
            latitude = 51.5072,
            longitude = -0.1276,
            accuracyMeters = 30f,
            capturedAtEpochMillis = TodayStart,
        )

        fun hours(count: Int): Long = count * 3_600_000L
    }
}

private class FakePrayerService : PrayerService {
    val cached = MutableStateFlow<PrayerSchedule?>(null)

    /** Per-date schedules, overriding [cached]. Only the midnight test needs them. */
    val byDate = mutableMapOf<CalendarDate, PrayerSchedule>()

    /** Every date the ViewModel has opened a query for, in order. */
    val observedDates = mutableListOf<CalendarDate>()

    var failure: Throwable? = null
    var refreshCount = 0
    var relocateCount = 0
    var hasCachedFix = true
    var locationFailure: Throwable? = null
    var lastForce = false
    var scheduleAlertsCount = 0

    override fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?> {
        observedDates += today
        return byDate[today]?.let { MutableStateFlow(it) } ?: cached
    }

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
        scheduleAlerts()
    }

    override suspend fun scheduleAlerts() {
        scheduleAlertsCount++
    }
}


private class MutableDateProvider(var date: CalendarDate) : DateProvider {
    override fun today(): CalendarDate = date
}

private class MutableTimeProvider : TimeProvider {
    var now: Long = 0L
    override fun nowEpochMillis(): Long = now
}
