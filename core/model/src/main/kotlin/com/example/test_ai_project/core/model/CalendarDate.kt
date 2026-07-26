package com.example.test_ai_project.core.model

/**
 * A date with no time and no zone — a date of birth is a calendar fact, not an instant.
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
}
