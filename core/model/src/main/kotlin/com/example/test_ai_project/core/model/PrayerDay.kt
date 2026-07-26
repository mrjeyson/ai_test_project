package com.example.test_ai_project.core.model

/**
 * When one prayer begins.
 *
 * [startEpochMillis] is an absolute instant, not a wall-clock time, and that is the whole
 * point of this type. A countdown subtracts from it, an alarm is set to it, and neither
 * can be done with "16:55" alone without re-deriving a time zone at every call site. The
 * zone is applied once, in the data layer, and never again.
 */
data class PrayerTime(
    val prayer: Prayer,
    val startEpochMillis: Long,
)

/**
 * One calendar day of prayer times, for one place.
 *
 * [latitude]/[longitude] travel with the day because the times are only valid for where
 * they were computed: a cached day is meaningless once the user has moved a city over, and
 * the repository can only notice that if the day remembers where it came from.
 *
 * [zoneId] is the *location's* zone, not the device's. They are the same in the ordinary
 * case, and when they are not — a cached day still on screen after a flight — formatting
 * against the device would silently shift every time on the list.
 *
 * [locationLabel] is nullable because reverse geocoding is a best-effort lookup that fails
 * offline. A day with no label is still a complete, correct day; the screen falls back to
 * coordinates rather than the repository refusing to cache one.
 */
data class PrayerDay(
    val date: CalendarDate,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val locationLabel: String?,
    /** Always the five of [Prayer], in chronological order. */
    val times: List<PrayerTime>,
    val fetchedAtEpochMillis: Long,
)

/**
 * Today's prayers plus tomorrow's, which together are what any "what is next" question
 * actually needs.
 *
 * Tomorrow is here for one reason: between Isha and midnight there is no next prayer today,
 * and a screen that can only see today has to answer "none" for the four hours when the
 * honest answer is "Fajr, at 03:57". The same gap would leave the device with no alarm set
 * overnight — precisely when the next alert is due.
 *
 * [tomorrow] is nullable because it is an optimisation, not a requirement: a first run that
 * fetched one day and then lost signal still renders today correctly.
 */
data class PrayerSchedule(
    val today: PrayerDay,
    val tomorrow: PrayerDay?,
) {
    /**
     * Every instant in the window, in chronological order.
     *
     * Sorted rather than assumed: the two days arrive from separate rows, and a schedule
     * built from a stale `tomorrow` left over from last week would otherwise interleave
     * silently and make the "next" prayer one that has already passed.
     */
    val allTimes: List<PrayerTime>
        get() = (today.times + tomorrow?.times.orEmpty()).sortedBy(PrayerTime::startEpochMillis)

    /** The prayers still ahead at [epochMillis], soonest first. Empty once the window runs out. */
    fun upcomingAfter(epochMillis: Long): List<PrayerTime> =
        allTimes.filter { it.startEpochMillis > epochMillis }

    /** The one the countdown counts down to, or null once the window runs out. */
    fun nextAfter(epochMillis: Long): PrayerTime? = upcomingAfter(epochMillis).firstOrNull()
}
