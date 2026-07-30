package com.example.test_ai_project.auth.domain.model

/**
 * A date with no time and no zone — a date of birth is a calendar fact, not an instant.
 *
 * Deliberately not `java.time.LocalDate`: `minSdk` is 24, and pulling in core library
 * desugaring for three integers is not a trade worth making.
 *
 * Auth's own type rather than a shared one. All this feature ever asks of a date is
 * "is it before today?", so it carries the comparison and nothing else — the instant-to-date
 * arithmetic the weather and prayer caches need has no meaning for a passport.
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
}
