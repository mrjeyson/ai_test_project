package com.example.test_ai_project.core.data.mapper

import com.example.test_ai_project.core.database.entity.PrayerDayEntity
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.Prayer
import com.example.test_ai_project.core.model.PrayerDay
import com.example.test_ai_project.core.model.PrayerTime
import com.example.test_ai_project.core.network.dto.PrayerTimingsDataDto
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Turns one day of Aladhan wall-clock strings into absolute instants.
 *
 * This is where the time zone stops travelling. Everything downstream — the countdown, the
 * ordering, the alarms — works on epoch millis, and none of it has to know that the
 * provider answered in local time for somewhere that may not be where the device is.
 *
 * @throws IllegalArgumentException if a timing is not `HH:mm`, which would otherwise be
 *   cached as an instant at midnight and quietly render as a prayer at 12:00 AM.
 */
internal fun PrayerTimingsDataDto.toEntity(
    date: CalendarDate,
    latitude: Double,
    longitude: Double,
    locationLabel: String?,
    fetchedAtEpochMillis: Long,
): PrayerDayEntity {
    val zone = resolveTimeZone(meta.timezone)
    fun at(time: String) = date.atLocalTime(time, zone)

    return PrayerDayEntity(
        date = date.toIsoString(),
        latitude = latitude,
        longitude = longitude,
        // The resolved zone's own id, not the string the provider sent: if that string was
        // unrecognisable the instants above were computed in a different zone, and storing
        // the one they were actually built from is what keeps formatting consistent
        // with them.
        zoneId = zone.id,
        locationLabel = locationLabel,
        fajrEpochMillis = at(timings.fajr),
        dhuhrEpochMillis = at(timings.dhuhr),
        asrEpochMillis = at(timings.asr),
        maghribEpochMillis = at(timings.maghrib),
        ishaEpochMillis = at(timings.isha),
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )
}

internal fun PrayerDayEntity.toDomain() = PrayerDay(
    date = date.toCalendarDate(),
    latitude = latitude,
    longitude = longitude,
    zoneId = zoneId,
    locationLabel = locationLabel,
    // Built in enum order, which is also chronological order, so nothing downstream has to
    // sort a list whose order is a property of the schema.
    times = listOf(
        PrayerTime(Prayer.Fajr, fajrEpochMillis),
        PrayerTime(Prayer.Dhuhr, dhuhrEpochMillis),
        PrayerTime(Prayer.Asr, asrEpochMillis),
        PrayerTime(Prayer.Maghrib, maghribEpochMillis),
        PrayerTime(Prayer.Isha, ishaEpochMillis),
    ),
    fetchedAtEpochMillis = fetchedAtEpochMillis,
)

/**
 * The instant at which [time] occurs on this date, in [zone].
 *
 * [Calendar.clear] before setting is not optional: a `Calendar` starts at the current
 * moment, and setting only the date and the hour would leave today's seconds and
 * milliseconds attached to it — enough to make an alarm fire up to a minute late.
 */
private fun CalendarDate.atLocalTime(time: String, zone: TimeZone): Long {
    // Aladhan appends the zone abbreviation on some queries — "16:55 (BST)" — and omits it
    // on others. Taking the head of the split handles both without a second code path.
    val parts = time.trim().substringBefore(' ').split(':')
    require(parts.size >= 2) { "Unrecognised prayer time: '$time'" }
    val hour = requireNotNull(parts[0].toIntOrNull()) { "Unrecognised prayer time: '$time'" }
    val minute = requireNotNull(parts[1].toIntOrNull()) { "Unrecognised prayer time: '$time'" }

    return Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, hour, minute, 0)
    }.timeInMillis
}

/**
 * [TimeZone.getTimeZone] answers GMT for anything it does not recognise, silently, which
 * would shift every prayer on the list by the local offset. Checking against the known ids
 * first turns that into a visible fallback to the device's own zone — wrong only for a user
 * reading a cached day from another country, rather than wrong for everyone.
 */
private fun resolveTimeZone(id: String): TimeZone =
    if (id in TimeZone.getAvailableIDs()) TimeZone.getTimeZone(id) else TimeZone.getDefault()

/** `yyyy-MM-dd` — the cache key, chosen because it sorts lexicographically. */
internal fun CalendarDate.toIsoString(): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

/** `dd-MM-yyyy` — Aladhan's path format, and only Aladhan's. */
internal fun CalendarDate.toAladhanPath(): String =
    String.format(Locale.US, "%02d-%02d-%04d", day, month, year)

/** Parses the `yyyy-MM-dd` written by [toIsoString]. Shared with the weather cache. */
internal fun String.toCalendarDate(): CalendarDate {
    val parts = split('-')
    return CalendarDate(
        year = parts[0].toInt(),
        month = parts[1].toInt(),
        day = parts[2].toInt(),
    )
}

/**
 * The day after this one.
 *
 * Delegated to [Calendar] rather than incrementing `day` and carrying: month lengths and
 * leap years are exactly the cases a hand-rolled version gets wrong, and it would get them
 * wrong once a year, in production, at midnight.
 *
 * UTC deliberately — this is arithmetic on a calendar date with no time and no place, so
 * involving a real zone would only introduce a boundary to be wrong about.
 */
internal fun CalendarDate.nextDay(): CalendarDate {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return CalendarDate(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
}
