package com.example.test_ai_project.home.domain.model


import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window logic every other layer leans on: the countdown asks it what is next, and the
 * alarm scheduler asks it what is still ahead. Both answers are wrong in the same way if
 * the boundary between the two days is.
 */
class PrayerScheduleTest {

    @Test
    fun `next prayer is the first one still ahead`() {
        val schedule = schedule(withTomorrow = true)

        val next = schedule.nextAfter(at(13, 28))

        assertEquals(Prayer.Asr, next?.prayer)
        assertEquals(TodayStart + hours(15.7), next?.startEpochMillis)
    }

    @Test
    fun `a prayer beginning exactly now has already begun`() {
        val schedule = schedule(withTomorrow = true)

        // Strictly-after, not at-or-after. At the instant Asr starts, Asr is no longer
        // something to count down to — it is the one that just fired the alert.
        val next = schedule.nextAfter(TodayStart + hours(15.7))

        assertEquals(Prayer.Maghrib, next?.prayer)
    }

    @Test
    fun `after the last prayer of the day the next one is tomorrow's first`() {
        val schedule = schedule(withTomorrow = true)

        // 22:00 — past Isha, and the gap the second cached day exists to cover.
        val next = schedule.nextAfter(at(22, 0))

        assertEquals(Prayer.Fajr, next?.prayer)
        assertEquals(TomorrowStart + hours(4.2), next?.startEpochMillis)
    }

    @Test
    fun `with no tomorrow cached the window simply runs out`() {
        val schedule = schedule(withTomorrow = false)

        assertNull(schedule.nextAfter(at(22, 0)))
        assertTrue(schedule.upcomingAfter(at(22, 0)).isEmpty())
    }

    @Test
    fun `upcoming spans both days in chronological order`() {
        val schedule = schedule(withTomorrow = true)

        val upcoming = schedule.upcomingAfter(at(13, 28))

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
            upcoming.map(PrayerTime::prayer),
        )
        assertEquals(
            upcoming.map(PrayerTime::startEpochMillis).sorted(),
            upcoming.map(PrayerTime::startEpochMillis),
        )
    }

    @Test
    fun `a stale second day cannot reorder the window`() {
        // The pathological case the sort in `allTimes` guards: a `tomorrow` row left over
        // from before a clock change, holding instants that precede today's.
        val stale = day(TodayStart - hours(24))
        val schedule = PrayerSchedule(today = day(TodayStart), tomorrow = stale)

        val times = schedule.allTimes

        assertEquals(times.map(PrayerTime::startEpochMillis).sorted(), times.map(PrayerTime::startEpochMillis))
        // And nothing from the stale day leaks into "upcoming", because it is all in the past.
        assertEquals(5, schedule.upcomingAfter(TodayStart).size)
    }

    private fun schedule(withTomorrow: Boolean) = PrayerSchedule(
        today = day(TodayStart),
        tomorrow = if (withTomorrow) day(TomorrowStart) else null,
    )

    private fun day(startOfDay: Long) = PrayerDay(
        date = CalendarDate(2026, 7, 26),
        latitude = 51.5072,
        longitude = -0.1276,
        zoneId = "Europe/London",
        locationLabel = "London, United Kingdom",
        times = listOf(
            PrayerTime(Prayer.Fajr, startOfDay + hours(4.2)),
            PrayerTime(Prayer.Dhuhr, startOfDay + hours(12.47)),
            PrayerTime(Prayer.Asr, startOfDay + hours(15.7)),
            PrayerTime(Prayer.Maghrib, startOfDay + hours(18.9)),
            PrayerTime(Prayer.Isha, startOfDay + hours(20.37)),
        ),
        fetchedAtEpochMillis = startOfDay,
    )

    private fun at(hour: Int, minute: Int) = TodayStart + hours(hour + minute / 60.0)

    private companion object {
        /** Local midnight, 26 July 2026, in London. */
        const val TodayStart = 1_785_020_400_000L
        val TomorrowStart = TodayStart + hours(24)

        fun hours(count: Double): Long = (count * 3_600_000L).toLong()
        fun hours(count: Int): Long = count * 3_600_000L
    }
}
