package com.example.test_ai_project.feature.home.prayertimes

import androidx.annotation.StringRes
import com.example.test_ai_project.core.model.Prayer

/**
 * Everything the Prayer tab renders.
 *
 * Flags rather than a sealed `Loading | Success | Error` hierarchy, for the same reason the
 * Movies and Map tabs use them: the states are not mutually exclusive. The ordinary case
 * here is *a cached timetable* and *a fetch in flight* and *the last attempt having
 * failed*, all at once.
 *
 * Note what is absent: the current time. This state is recomputed on every tick, and
 * folding `now` into it would make it a different value every second — which, published
 * through a `StateFlow`, would recompose the whole page once a second for a countdown that
 * lives in one composable. Keeping `now` out means this only changes when something the eye
 * can see changes: a prayer moving from upcoming to completed. The countdown itself
 * travels separately.
 */
data class PrayerTimesUiState(
    /** Today's five, always in chronological order. Empty until a day is cached. */
    val entries: List<PrayerEntry> = emptyList(),
    /** What the countdown counts down to. Null once the cached window runs out. */
    val next: NextPrayer? = null,
    /** A place name if reverse geocoding found one, coordinates if it did not. */
    val locationLabel: String? = null,
    /** The zone the times belong to — not necessarily the device's. Formatting needs it. */
    val zoneId: String? = null,
    val lastUpdatedEpochMillis: Long? = null,
    /** A fetch is in flight. The cached timetable stays on screen while it runs. */
    val isLoading: Boolean = false,
    /** Non-null when the last attempt failed. Advisory: the timetable is still whatever is cached. */
    @param:StringRes val messageRes: Int? = null,
    /** Offered instead of a retry when the failure was a refused location permission. */
    val isPermissionRequestable: Boolean = false,
) {
    /** There is a timetable on screen, and it came from disk — so it survives going offline. */
    val isCached: Boolean get() = entries.isNotEmpty()

    /** No cache to fall back on, so the spinner gets the whole page rather than a strip. */
    val isInitialLoad: Boolean get() = entries.isEmpty() && isLoading

    /** Nothing cached and nothing in flight — the only genuinely empty state. */
    val isEmpty: Boolean get() = entries.isEmpty() && !isLoading
}

/**
 * One row of the daily schedule.
 *
 * [startEpochMillis] rather than a preformatted string: whether the device shows a 12- or
 * 24-hour clock is a question only the UI can answer, and answering it here would mean
 * a ViewModel that needs a `Context`.
 */
data class PrayerEntry(
    val prayer: Prayer,
    val startEpochMillis: Long,
    val status: PrayerStatus,
)


enum class PrayerStatus {
    Completed,
    Next,
    Later,
}


data class NextPrayer(
    val prayer: Prayer,
    val startEpochMillis: Long,
    val isTomorrow: Boolean,
)
