package com.example.test_ai_project.home.presentation.weather.contract

import androidx.annotation.StringRes
import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.CurrentWeather
import com.example.test_ai_project.home.domain.model.WeatherCondition
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the Weather tab renders.
 *
 * Flags rather than a sealed `Loading | Success | Error` hierarchy, for the reason the other
 * three tabs use them: the states are not mutually exclusive. The ordinary case here is *a
 * cached snapshot* and *a fetch in flight* and *the last attempt having failed*, all at once.
 *
 * Unlike [com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesState], this state
 * does fold the clock in — [isStale] and the 24-hour window in [hourly] are both functions of
 * "now". It can afford to: prayer times need a per-second countdown, so a clock in the state
 * would recompose the page once a second, whereas nothing here changes faster than the
 * quarter-hour tick that drives it.
 *
 * [current] is the domain type unchanged. There is no parallel UI copy of it because there
 * would be nothing to add — every field is displayed as-is, and the unit conversions the design
 * asks for (m/s to km/h, metres to km) are presentation and live in the composable.
 */
data class WeatherState(
    /** A place name if one was found, coordinates if not. */
    val place: String? = null,
    /** Seconds to add to UTC for the location's clock. Formatting the hourly strip needs it. */
    val zoneOffsetSeconds: Int? = null,
    /** Null until a snapshot has ever been cached. */
    val current: CurrentWeather? = null,
    /** The "Next 24h" strip, starting with a "now" column. Empty if only current was cached. */
    val hourly: List<HourlyColumn> = emptyList(),
    /** Chronological, starting with today. Empty if only current conditions were cached. */
    val daily: List<DailyRow> = emptyList(),
    val lastUpdatedEpochMillis: Long? = null,
    /**
     * The snapshot is old enough that the screen should say so rather than present it as the
     * present.
     *
     * The distinction the other cached tabs do not need: a cached prayer time is still correct
     * tomorrow, and a cached temperature is only ever the last one known.
     */
    val isStale: Boolean = false,
    /** A fetch is in flight. The cached snapshot stays on screen while it runs. */
    val isLoading: Boolean = false,
    /** Non-null when the last attempt failed. Advisory: the readings are still whatever is cached. */
    @param:StringRes val messageRes: Int? = null,
    /** Offered instead of a retry when the failure was a refused location permission. */
    val isPermissionRequestable: Boolean = false,
) : UiState {
    /** There is a reading on screen, and it came from disk — so it survives going offline. */
    val isCached: Boolean get() = current != null

    /** No cache to fall back on, so the spinner gets the whole page rather than a strip. */
    val isInitialLoad: Boolean get() = current == null && isLoading

    /** Nothing cached and nothing in flight — the only genuinely empty state. */
    val isEmpty: Boolean get() = current == null && !isLoading
}

/**
 * One column of the short-range strip.
 *
 * [isNow] marks the leading column, which is built from current conditions rather than from a
 * forecast step. It earns a flag rather than a sentinel timestamp because the screen labels it
 * differently — "Now" against a clock time — and a magic value would push that decision into
 * the composable as a comparison against the current time it would then also need.
 */
data class HourlyColumn(
    val startEpochMillis: Long,
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val isNight: Boolean,
    val isNow: Boolean,
)

/**
 * One row of the multi-day list.
 *
 * [barStartFraction] and [barEndFraction] place this day's range on the shared track the design
 * draws, as fractions of the whole list's span. Computed in the ViewModel rather than the
 * composable because they depend on every *other* row — the minimum and maximum across the
 * list — which is precisely the kind of cross-item derivation a row-scoped composable cannot do
 * without being handed the list it is one row of.
 */
data class DailyRow(
    val date: CalendarDate,
    val highCelsius: Double,
    val lowCelsius: Double,
    val condition: WeatherCondition,
    val isToday: Boolean,
    /** 0f at the coldest low in the list, 1f at the warmest high. */
    val barStartFraction: Float,
    val barEndFraction: Float,
)

sealed interface WeatherEvent : UiEvent {

    /** The location permission dialogs have been answered, or found already answered. */
    data object PermissionsResolved : WeatherEvent

    /** Fetch again without moving the location. */
    data object RetryRequested : WeatherEvent

    /** Re-detect where the device is, then refetch. */
    data object RefreshRequested : WeatherEvent

    data object MessageDismissed : WeatherEvent
}
