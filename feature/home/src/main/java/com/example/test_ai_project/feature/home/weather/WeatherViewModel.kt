package com.example.test_ai_project.feature.home.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.core.domain.exception.LocationUnavailableException
import com.example.test_ai_project.core.domain.exception.WeatherKeyRejectedException
import com.example.test_ai_project.core.domain.exception.WeatherServiceNotConfiguredException
import com.example.test_ai_project.core.domain.time.TimeProvider
import com.example.test_ai_project.core.domain.usecase.GetWeatherUseCase
import com.example.test_ai_project.core.domain.usecase.RefreshWeatherUseCase
import com.example.test_ai_project.core.model.WeatherSnapshot
import com.example.test_ai_project.feature.home.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(
    getWeather: GetWeatherUseCase,
    private val refreshWeather: RefreshWeatherUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val messageRes = MutableStateFlow<Int?>(null)
    private val isPermissionRequestable = MutableStateFlow(false)

    /**
     * The clock, as a flow — but a far coarser one than the prayer screen's.
     *
     * A quarter-hour, because that is the resolution of everything on this page that depends on
     * the time: whether the snapshot has aged into "stale", and which forecast steps fall in the
     * next 24 hours. The steps themselves are three hours apart, so ticking any faster would
     * recompute the same list.
     *
     * Injected through [TimeProvider] rather than read from `System` so the staleness banner and
     * the 24-hour window can be tested at any moment without waiting for one.
     */
    private val now: StateFlow<Long> = flow {
        while (true) {
            emit(timeProvider.nowEpochMillis())
            delay(TICK_INTERVAL_MILLIS)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = timeProvider.nowEpochMillis(),
    )

    /**
     * The cached snapshot, mapped for the screen.
     *
     * No `flatMapLatest` over a date, unlike the prayer schedule: there is one cached snapshot
     * and it is always the latest, so nothing about the passing of time changes *which* rows are
     * read — only how their age is described.
     */
    val uiState: StateFlow<WeatherUiState> = combine(
        getWeather(),
        now,
        isLoading,
        messageRes,
        isPermissionRequestable,
    ) { snapshot, now, loading, message, permissionRequestable ->
        snapshot.toUiState(
            nowEpochMillis = now,
            isLoading = loading,
            messageRes = message,
            isPermissionRequestable = permissionRequestable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WeatherUiState(),
    )

    private var loadJob: Job? = null

    /**
     * The permission dialogs have been answered, or found already answered.
     *
     * Takes no grant result, deliberately: a load follows either way. The forecast needs
     * *coordinates*, not a live fix, and the Map or Prayer tabs may already have cached some — so
     * a refusal still produces a working screen for anyone who has used those, and for anyone who
     * has not, the attempt is what surfaces the failure that offers the grant.
     */
    fun onPermissionsResolved() {
        load(relocate = false)
    }

    /** Explicit user action — fetch again without moving the location. */
    fun retry() = load(relocate = false)

    /**
     * The refresh control: re-detect where the device is and refetch.
     *
     * Passes `relocate`, which is what bypasses the repository's freshness window. A user who
     * taps refresh is telling us the reading on screen is not good enough, and honouring that
     * with "the cache is only nine minutes old" would make the button look broken.
     */
    fun refresh() = load(relocate = true)

    fun dismissMessage() {
        messageRes.value = null
        isPermissionRequestable.value = false
    }

    private fun load(relocate: Boolean) {
        // Cancelling matters: tapping refresh while a first load is still waiting on a GPS lock
        // would otherwise leave two attempts racing to clear the loading flag and the banner out
        // from under each other.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            runCatching { refreshWeather(relocate = relocate) }
                .onSuccess {
                    messageRes.value = null
                    isPermissionRequestable.value = false
                }
                .onFailure { error ->
                    messageRes.value = error.toMessageRes()
                    // Only a refusal is worth offering a grant for. Anything else would put an
                    // "Allow" button in front of a problem it cannot fix.
                    isPermissionRequestable.value = error is LocationPermissionDeniedException
                }
            isLoading.value = false
        }
    }

    private fun WeatherSnapshot?.toUiState(
        nowEpochMillis: Long,
        isLoading: Boolean,
        messageRes: Int?,
        isPermissionRequestable: Boolean,
    ): WeatherUiState {
        if (this == null) {
            return WeatherUiState(
                isLoading = isLoading,
                messageRes = messageRes,
                isPermissionRequestable = isPermissionRequestable,
            )
        }

        return WeatherUiState(
            place = displayPlace(),
            zoneOffsetSeconds = zoneOffsetSeconds,
            current = current,
            hourly = hourlyColumns(nowEpochMillis),
            daily = dailyRows(nowEpochMillis),
            lastUpdatedEpochMillis = fetchedAtEpochMillis,
            isStale = nowEpochMillis - fetchedAtEpochMillis > STALE_AFTER_MILLIS,
            isLoading = isLoading,
            messageRes = messageRes,
            isPermissionRequestable = isPermissionRequestable,
        )
    }

    /**
     * The strip: current conditions as the leading column, then the forecast steps that fall in
     * the next 24 hours.
     *
     * The leading column is synthesised here rather than stored, because "now" is not a forecast
     * step — the provider's window starts at the next one. Building it from [WeatherSnapshot.current]
     * is what makes the strip start where the user is standing instead of up to three hours ahead.
     */
    private fun WeatherSnapshot.hourlyColumns(nowEpochMillis: Long): List<HourlyColumn> {
        val upcoming = hourlyWithin(nowEpochMillis, HOURLY_WINDOW_MILLIS)
        // No forecast at all means the strip has nothing to say beyond what the header already
        // shows, so it renders empty rather than as a single lonely "Now" column.
        if (upcoming.isEmpty()) return emptyList()

        return buildList {
            add(
                HourlyColumn(
                    // The reading's own age is already shown in the status strip, so this column
                    // is stamped with the time it is being read at rather than fetched at — it is
                    // labelled "Now", and a timestamp that disagreed with that would be worse
                    // than one nothing displays.
                    startEpochMillis = nowEpochMillis,
                    temperatureCelsius = current.temperatureCelsius,
                    condition = current.condition,
                    isNight = current.isNight,
                    isNow = true,
                ),
            )
            upcoming.forEach { step ->
                add(
                    HourlyColumn(
                        startEpochMillis = step.startEpochMillis,
                        temperatureCelsius = step.temperatureCelsius,
                        condition = step.condition,
                        isNight = step.isNight,
                        isNow = false,
                    ),
                )
            }
        }
    }

    /**
     * The multi-day list, with each row's range placed on the shared track.
     *
     * The fractions are computed against the span of the list, which is what makes the bars
     * comparable to each other — a bar that filled its own row's range would be full width on every
     * row and carry no information at all.
     */
    private fun WeatherSnapshot.dailyRows(nowEpochMillis: Long): List<DailyRow> {
        // Derived from the clock and the location's offset, not taken to be the first row. The
        // forecast window opens at the next three-hour step, so a snapshot fetched just before
        // local midnight has no row for today at all and its first row is tomorrow — which "the
        // first row is today" would then label "Today" on the one evening it is wrong.
        val today = localDateAt(nowEpochMillis)

        // Days that have already been and gone are dropped rather than rendered, for the same
        // reason the hourly strip drops steps behind the clock. The cache is written once and read
        // against a clock that keeps moving, so after a few days offline these rows are last week's
        // — and rendered as bare weekday names, "Mon" would read as the Monday coming.
        val upcoming = daily.filter { it.date >= today }
        if (upcoming.isEmpty()) return emptyList()

        // Scaled over what is actually shown, not over the whole cache: a track whose ends were set
        // by days no longer on screen would leave every visible bar bunched in the middle.
        val coldest = upcoming.minOf { it.lowCelsius }
        val warmest = upcoming.maxOf { it.highCelsius }
        // Guarded because a flat week is a real forecast, not a bad one — Reykjavík in February can
        // genuinely return the same degree all week, and dividing by that span would put a NaN in
        // every bar.
        val span = max(warmest - coldest, MIN_BAR_SPAN_CELSIUS)

        return upcoming.map { day ->
            DailyRow(
                date = day.date,
                highCelsius = day.highCelsius,
                lowCelsius = day.lowCelsius,
                condition = day.condition,
                isToday = day.date == today,
                barStartFraction = ((day.lowCelsius - coldest) / span).toFloat(),
                barEndFraction = ((day.highCelsius - coldest) / span).toFloat(),
            )
        }
    }

    /**
     * Coordinates when neither the provider nor the geocoder produced a name.
     *
     * Ugly on purpose, and better than the alternatives — the same call the prayer screen makes:
     * a blank row makes the page look broken, and "Unknown location" is less informative than
     * four decimal places a user can sanity-check against where they think they are.
     *
     * [Locale.US] rather than the device locale, because coordinates are conventionally written
     * with a decimal point everywhere — "64,1466, -21,9426" would read as four numbers.
     */
    private fun WeatherSnapshot.displayPlace(): String =
        placeName ?: String.format(Locale.US, "%.4f, %.4f", latitude, longitude)

    private fun Throwable.toMessageRes(): Int = when (this) {
        // The two configuration faults come first. Neither resolves itself however many times the
        // user retries, and they are told apart because the remedies differ: one is a key to add,
        // the other a key to wait on.
        is WeatherServiceNotConfiguredException -> R.string.weather_error_not_configured
        is WeatherKeyRejectedException -> R.string.weather_error_key_rejected
        // Before the two below: fixed by a grant the app can ask for, and retrying without
        // asking would fail identically forever.
        is LocationPermissionDeniedException -> R.string.weather_error_permission
        is LocationUnavailableException -> R.string.weather_error_no_location
        is IOException -> R.string.weather_error_unreachable
        else -> R.string.weather_error_generic
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /**
         * A quarter-hour. The forecast moves in three-hour steps and the staleness threshold is
         * an hour, so this is already finer than anything it drives — it is the interval at which
         * the "Next 24h" window is allowed to have drifted, not a clock.
         */
        const val TICK_INTERVAL_MILLIS = 15 * 60 * 1_000L

        /**
         * An hour. Long enough that a user reading the page over breakfast is not told their
         * temperature is old, short enough that yesterday's reading is never presented as today's
         * weather.
         */
        const val STALE_AFTER_MILLIS = 60 * 60 * 1_000L

        const val HOURLY_WINDOW_MILLIS = 24 * 60 * 60 * 1_000L

        /** One degree. Below this the bars stop being informative and start being noise. */
        const val MIN_BAR_SPAN_CELSIUS = 1.0
    }
}
