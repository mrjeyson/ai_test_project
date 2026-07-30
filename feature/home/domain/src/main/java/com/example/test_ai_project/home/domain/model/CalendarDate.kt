package com.example.test_ai_project.home.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * A date with no time and no zone — "which day is this forecast row for" is a calendar
 * fact, not an instant.
 *
 * Deliberately not `java.time.LocalDate`: `minSdk` is 24, and pulling in core library
 * desugaring for three integers is not a trade worth making.
 */
data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<CalendarDate> {

    override fun compareTo(other: CalendarDate): Int = when {
        year != other.year -> year - other.year
        month != other.month -> month - other.month
        else -> day - other.day
    }

    companion object {
        /**
         * The date [epochMillis] falls on, for an observer whose clock is [zoneOffsetSeconds] ahead
         * of UTC.
         *
         * Shifts the instant by the offset and reads the fields back in UTC. That is the whole
         * trick, and it is what lets this work from a raw offset with no zone name to look up —
         * which is all some providers give. A shifted instant read in UTC has exactly the
         * wall-clock fields of the unshifted instant read at the offset.
         *
         * Lives in the domain rather than in either layer that needs it. The data layer buckets a
         * forecast by the location's date and the UI asks whether a row is today; those have to
         * agree, and two copies of this arithmetic are how they would stop agreeing.
         */
        fun at(epochMillis: Long, zoneOffsetSeconds: Int): CalendarDate {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = epochMillis + zoneOffsetSeconds * MILLIS_PER_SECOND
            }
            return CalendarDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
            )
        }

        private const val MILLIS_PER_SECOND = 1_000L
    }
}
