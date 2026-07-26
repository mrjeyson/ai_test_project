package com.example.test_ai_project.feature.home.prayertimes

import com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.core.domain.notification.PrayerAlarmScheduler
import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.domain.time.TimeProvider
import com.example.test_ai_project.core.domain.usecase.GetPrayerScheduleUseCase
import com.example.test_ai_project.core.domain.usecase.RefreshPrayerTimesUseCase
import com.example.test_ai_project.core.domain.usecase.SchedulePrayerAlertsUseCase
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.Prayer
import com.example.test_ai_project.core.model.PrayerDay
import com.example.test_ai_project.core.model.PrayerSchedule
import com.example.test_ai_project.core.model.PrayerTime
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.feature.home.R
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

/**
 * The real use cases are used rather than fakes — they are thin orchestration over the
 * repositories, and doubling them would mean testing the ViewModel against a version of the
 * rules written for the test. The repositories and the alarm scheduler, which are the parts
 * with I/O, are what gets faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prayerRepository = FakePrayerTimesRepository()
    private val locationRepository = FakeLocationRepository()
    private val alarmScheduler = RecordingPrayerAlarmScheduler()
    private val clock = MutableTimeProvider()

    private val dateProvider = MutableDateProvider(Today)

    private fun viewModel(): PrayerTimesViewModel {
        return PrayerTimesViewModel(
            getPrayerSchedule = GetPrayerScheduleUseCase(prayerRepository),
            refreshPrayerTimes = RefreshPrayerTimesUseCase(
                locationRepository = locationRepository,
                prayerTimesRepository = prayerRepository,
                schedulePrayerAlerts = SchedulePrayerAlertsUseCase(
                    prayerTimesRepository = prayerRepository,
                    prayerAlarmScheduler = alarmScheduler,
                    dateProvider = dateProvider,
                    timeProvider = clock,
                ),
                dateProvider = dateProvider,
            ),
            dateProvider = dateProvider,
            timeProvider = clock,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        locationRepository.cached.value = Fix
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `marks past prayers completed, the soonest next, and the rest later`() = runTest {
        prayerRepository.cached.value = twoDaySchedule()
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
        prayerRepository.cached.value = twoDaySchedule()
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
        prayerRepository.cached.value = twoDaySchedule()
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
        prayerRepository.cached.value = twoDaySchedule()
        prayerRepository.failure = IOException("offline")
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionsResolved()
        settle()

        assertEquals(R.string.prayer_error_unreachable, viewModel.uiState.value.messageRes)
        // The whole point of the cache: the failure does not blank the page.
        assertEquals(5, viewModel.uiState.value.entries.size)
        assertFalse(viewModel.uiState.value.isLoading)
        // Nothing to offer a grant for — this is a network problem, not a permission one.
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refused location permission offers the grant instead of a retry`() = runTest {
        locationRepository.cached.value = null
        locationRepository.failure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionsResolved()
        settle()

        assertEquals(R.string.prayer_error_permission, viewModel.uiState.value.messageRes)
        assertTrue(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `a refusal still works from a fix the map already cached`() = runTest {
        // No permission this session, but coordinates are already on disk — so the
        // timetable can be fetched without ever asking the platform for a new fix.
        locationRepository.failure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionsResolved()
        settle()

        assertEquals(0, locationRepository.refreshCount)
        assertEquals(1, prayerRepository.refreshCount)
        assertNull(viewModel.uiState.value.messageRes)
    }

    @Test
    fun `CHANGE re-detects the location and bypasses the freshness check`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.changeLocation()
        settle()

        assertEquals(1, locationRepository.refreshCount)
        assertTrue(prayerRepository.lastForce)
    }

    @Test
    fun `an ordinary load reuses the cached fix rather than acquiring one`() = runTest {
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.retry()
        settle()

        assertEquals(0, locationRepository.refreshCount)
        assertFalse(prayerRepository.lastForce)
    }

    @Test
    fun `a successful refresh re-derives the alarms`() = runTest {
        prayerRepository.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionsResolved()
        settle()

        assertEquals(1, alarmScheduler.callCount)
        assertEquals(Prayer.Asr, alarmScheduler.lastAlerts.first().prayer)
    }

    @Test
    fun `a failed refresh leaves the existing alarms alone`() = runTest {
        prayerRepository.cached.value = twoDaySchedule()
        prayerRepository.failure = IOException("offline")
        val viewModel = viewModel()
        observe(viewModel)

        viewModel.onPermissionsResolved()
        settle()

        // Rewriting them from a fetch that failed could only make them worse: the alarms
        // already pending were derived from the same cache that is still on screen.
        assertEquals(0, alarmScheduler.callCount)
    }

    @Test
    fun `dismissing clears the message and the grant offer together`() = runTest {
        locationRepository.cached.value = null
        locationRepository.failure = LocationPermissionDeniedException()
        val viewModel = viewModel()
        observe(viewModel)
        viewModel.onPermissionsResolved()
        settle()

        viewModel.dismissMessage()
        settle()

        assertNull(viewModel.uiState.value.messageRes)
        assertFalse(viewModel.uiState.value.isPermissionRequestable)
    }

    @Test
    fun `crossing midnight switches the page to the new day`() = runTest {
        prayerRepository.byDate[Today] = twoDaySchedule()
        prayerRepository.byDate[Tomorrow] =
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
        assertEquals(listOf(Today, Tomorrow), prayerRepository.observedDates)
    }

    @Test
    fun `a tick that does not cross midnight does not requery`() = runTest {
        prayerRepository.cached.value = twoDaySchedule()
        clock.now = TodayStart + hours(13)
        val viewModel = viewModel()
        observe(viewModel)
        settle()

        repeat(5) { tick() }

        // One query, not six: the date is distinct-until-changed, so a second passing costs
        // nothing at the database.
        assertEquals(listOf(Today), prayerRepository.observedDates)
    }

    @Test
    fun `falls back to coordinates when the place has no name`() = runTest {
        prayerRepository.cached.value = PrayerSchedule(
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

private class FakePrayerTimesRepository : PrayerTimesRepository {
    val cached = MutableStateFlow<PrayerSchedule?>(null)

    /** Per-date schedules, overriding [cached]. Only the midnight test needs them. */
    val byDate = mutableMapOf<CalendarDate, PrayerSchedule>()

    /** Every date the ViewModel has opened a query for, in order. */
    val observedDates = mutableListOf<CalendarDate>()

    var failure: Throwable? = null
    var refreshCount = 0
    var lastForce = false

    override fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?> {
        observedDates += today
        return byDate[today]?.let { MutableStateFlow(it) } ?: cached
    }

    override suspend fun schedule(today: CalendarDate): PrayerSchedule? = cached.value

    override suspend fun refresh(location: UserLocation, today: CalendarDate, force: Boolean) {
        refreshCount++
        lastForce = force
        failure?.let { throw it }
    }
}

private class FakeLocationRepository : LocationRepository {
    val cached = MutableStateFlow<UserLocation?>(null)
    var failure: Throwable? = null
    var refreshCount = 0

    override fun observeLastKnownLocation(): Flow<UserLocation?> = cached

    override suspend fun refreshCurrentLocation() {
        refreshCount++
        failure?.let { throw it }
    }
}

private class RecordingPrayerAlarmScheduler : PrayerAlarmScheduler {
    var lastAlerts: List<PrayerTime> = emptyList()
    var callCount = 0

    override suspend fun replaceAlerts(times: List<PrayerTime>) {
        lastAlerts = times
        callCount++
    }
}

private class MutableDateProvider(var date: CalendarDate) : DateProvider {
    override fun today(): CalendarDate = date
}

private class MutableTimeProvider : TimeProvider {
    var now: Long = 0L
    override fun nowEpochMillis(): Long = now
}
