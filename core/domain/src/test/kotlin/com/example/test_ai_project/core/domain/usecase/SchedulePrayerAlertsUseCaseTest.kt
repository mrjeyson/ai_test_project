package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.notification.PrayerAlarmScheduler
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import com.example.test_ai_project.core.domain.time.TimeProvider
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.Prayer
import com.example.test_ai_project.core.model.PrayerDay
import com.example.test_ai_project.core.model.PrayerSchedule
import com.example.test_ai_project.core.model.PrayerTime
import com.example.test_ai_project.core.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule under test is one sentence — *the alerts are the prayers the cache still has
 * ahead of now* — and every way of getting it wrong is a missed prayer alert or one that
 * fires at the wrong time.
 */
class SchedulePrayerAlertsUseCaseTest {

    private val repository = FakePrayerTimesRepository()
    private val scheduler = RecordingPrayerAlarmScheduler()
    private val clock = MutableTimeProvider()

    private val useCase = SchedulePrayerAlertsUseCase(
        prayerTimesRepository = repository,
        prayerAlarmScheduler = scheduler,
        dateProvider = FixedDateProvider(Today),
        timeProvider = clock,
    )

    @Test
    fun `schedules only the prayers still ahead`() = runTest {
        repository.schedule = twoDaySchedule()
        clock.now = TodayStart + hours(13)

        useCase()

        // Fajr and Dhuhr have passed; the rest of today plus all of tomorrow remain.
        assertEquals(
            listOf(
                Prayer.Asr,
                Prayer.Maghrib,
                Prayer.Isha,
                Prayer.Fajr,
                Prayer.Dhuhr,
                Prayer.Asr,
                Prayer.Maghrib,
                Prayer.Isha,
            ),
            scheduler.lastAlerts.map(PrayerTime::prayer),
        )
    }

    @Test
    fun `never schedules more than the pending limit`() = runTest {
        repository.schedule = twoDaySchedule()
        clock.now = TodayStart - hours(1)

        useCase()

        assertEquals(
            PrayerAlarmScheduler.MAX_PENDING_ALERTS,
            scheduler.lastAlerts.size,
        )
        // And what survives the cap is the *soonest* ten, not an arbitrary ten.
        assertEquals(TodayStart + hours(4), scheduler.lastAlerts.first().startEpochMillis)
    }

    @Test
    fun `an empty cache clears every pending alert`() = runTest {
        repository.schedule = null

        useCase()

        // The call still happens — that is what cancels alarms belonging to a schedule the
        // cache no longer has. Skipping it would leave them pending forever.
        assertEquals(1, scheduler.callCount)
        assertTrue(scheduler.lastAlerts.isEmpty())
    }

    @Test
    fun `a fully elapsed day clears every pending alert`() = runTest {
        repository.schedule = PrayerSchedule(today = day(TodayStart), tomorrow = null)
        clock.now = TodayStart + hours(23)

        useCase()

        assertEquals(1, scheduler.callCount)
        assertTrue(scheduler.lastAlerts.isEmpty())
    }

    @Test
    fun `reads the schedule for today, not for whatever date is lying around`() = runTest {
        repository.schedule = twoDaySchedule()
        clock.now = TodayStart

        useCase()

        assertEquals(Today, repository.requestedDate)
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

    private companion object {
        val Today = CalendarDate(2026, 7, 26)

        /** Local midnight, 26 July 2026, in London. */
        const val TodayStart = 1_785_020_400_000L

        fun hours(count: Int): Long = count * 3_600_000L
    }
}

private class FakePrayerTimesRepository : PrayerTimesRepository {
    var schedule: PrayerSchedule? = null
    var requestedDate: CalendarDate? = null

    override fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?> =
        MutableStateFlow(schedule)

    override suspend fun schedule(today: CalendarDate): PrayerSchedule? {
        requestedDate = today
        return schedule
    }

    override suspend fun refresh(location: UserLocation, today: CalendarDate, force: Boolean) = Unit
}

private class RecordingPrayerAlarmScheduler : PrayerAlarmScheduler {
    var lastAlerts: List<PrayerTime> = emptyList()
    var callCount = 0

    override suspend fun replaceAlerts(times: List<PrayerTime>) {
        lastAlerts = times
        callCount++
    }
}

private class FixedDateProvider(private val today: CalendarDate) : DateProvider {
    override fun today(): CalendarDate = today
}

private class MutableTimeProvider : TimeProvider {
    var now: Long = 0L
    override fun nowEpochMillis(): Long = now
}
