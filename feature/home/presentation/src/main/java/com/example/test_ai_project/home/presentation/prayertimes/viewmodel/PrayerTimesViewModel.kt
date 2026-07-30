package com.example.test_ai_project.home.presentation.prayertimes.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.home.domain.exception.LocationPermissionDeniedException
import com.example.test_ai_project.home.domain.exception.LocationUnavailableException
import com.example.test_ai_project.home.domain.service.DateProvider
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.PrayerDay
import com.example.test_ai_project.home.domain.model.PrayerSchedule
import com.example.test_ai_project.home.domain.service.PrayerService
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesEvent
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerTimesState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.base.BaseViewModel
import com.example.test_ai_project.resource.base.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.test_ai_project.home.presentation.prayertimes.contract.NextPrayer
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerEntry
import com.example.test_ai_project.home.presentation.prayertimes.contract.PrayerStatus

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerService: PrayerService,
    private val dateProvider: DateProvider,
    private val timeProvider: TimeProvider,
) : BaseViewModel<PrayerTimesState, PrayerTimesEvent, NoEffect>(PrayerTimesState()) {

    private val isLoading = MutableStateFlow(false)
    private val messageRes = MutableStateFlow<Int?>(null)
    private val isPermissionRequestable = MutableStateFlow(false)

    /**
     * The clock, as a flow.
     *
     * Injected through [TimeProvider] rather than read from `System` so the whole screen
     * can be tested at any moment of the day — the difference between "Asr is next" and
     * "everything is completed" is entirely a function of what this emits.
     *
     * Shared as a [StateFlow] so the two consumers below drive one ticker between them,
     * and `WhileSubscribed` so it stops the moment the tab is left. A second-resolution
     * clock that keeps running behind another tab is a wake-up a second, forever.
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
     * The cached schedule for whatever day it currently is.
     *
     * The date is re-read on every tick and pushed through [distinctUntilChanged], so the
     * database query underneath is swapped exactly once per midnight and not once per
     * second. Without this the page would keep counting down against the day it was opened
     * on — and it is a page people leave open overnight and look at again at dawn, which is
     * precisely when the stale answer would be wrong.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val schedule: StateFlow<PrayerSchedule?> = now
        .map { dateProvider.today() }
        .distinctUntilChanged()
        .flatMapLatest { today -> prayerService.observeSchedule(today) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    /**
     * Recomputed every tick, but published only when it actually differs — [StateFlow]
     * drops a value equal to the one before it, and this state deliberately holds no clock.
     * So a second passing costs one comparison, and a prayer beginning costs one emission.
     */
    override val uiState: StateFlow<PrayerTimesState> = combine(
        schedule,
        now,
        isLoading,
        messageRes,
        isPermissionRequestable,
    ) { schedule, now, loading, message, permissionRequestable ->
        schedule.toUiState(
            nowEpochMillis = now,
            isLoading = loading,
            messageRes = message,
            isPermissionRequestable = permissionRequestable,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PrayerTimesState(),
        )

    override fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.PermissionsResolved -> onPermissionsResolved()
            PrayerTimesEvent.RetryRequested -> retry()
            PrayerTimesEvent.LocationChangeRequested -> changeLocation()
            PrayerTimesEvent.MessageDismissed -> dismissMessage()
        }
    }

    /**
     * Milliseconds until the next prayer, or null when the cache has nothing ahead.
     *
     * Separate from [uiState] precisely because it *does* change every second. Kept apart,
     * only the countdown recomposes on a tick; folded in, the whole page would.
     */
    val remainingMillis: StateFlow<Long?> = combine(schedule, now) { schedule, now ->
        schedule?.nextAfter(now)?.let { next -> next.startEpochMillis - now }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
    )

    private var loadJob: Job? = null

    /**
     * The permission dialogs have been answered, or found already answered.
     *
     * Takes no grant result, and that is deliberate: a load follows either way. The
     * timetable needs *coordinates*, not a live fix, and the Map tab may already have
     * cached some — so a refusal still produces a working screen for anyone who has used
     * the map, and for anyone who has not, the attempt is what surfaces the failure that
     * offers the grant.
     */
    private fun onPermissionsResolved() {
        load(relocate = false)
    }

    /** Explicit user action — pull the timetable again without moving the location. */
    private fun retry() = load(relocate = false)

    /**
     * The CHANGE control on the location row: re-detect where the device is and refetch.
     *
     * The only caller that passes `relocate`, and the reason the flag exists — it is the
     * one moment the app has been told the cached coordinates are stale, which is the
     * exact input the repository's distance check would otherwise trust.
     */
    private fun changeLocation() = load(relocate = true)

    private fun dismissMessage() {
        messageRes.value = null
        isPermissionRequestable.value = false
    }

    private fun load(relocate: Boolean) {
        // Cancelling matters: tapping CHANGE while a first load is still waiting on a GPS
        // lock would otherwise leave two attempts racing to clear the loading flag and the
        // banner out from under each other.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            runCatching { prayerService.refresh(relocate = relocate) }
                .onSuccess {
                    messageRes.value = null
                    isPermissionRequestable.value = false
                }
                .onFailure { error ->
                    messageRes.value = error.toMessageRes()
                    // Only a refusal is worth offering a grant for. Anything else would
                    // put an "Allow" button in front of a problem it cannot fix.
                    isPermissionRequestable.value = error is LocationPermissionDeniedException
                }
            isLoading.value = false
        }
    }

    private fun PrayerSchedule?.toUiState(
        nowEpochMillis: Long,
        isLoading: Boolean,
        messageRes: Int?,
        isPermissionRequestable: Boolean,
    ): PrayerTimesState {
        val today = this?.today
        val next = this?.nextAfter(nowEpochMillis)

        return PrayerTimesState(
            entries = today?.times.orEmpty().map { time ->
                PrayerEntry(
                    prayer = time.prayer,
                    startEpochMillis = time.startEpochMillis,
                    status = when {
                        time.startEpochMillis <= nowEpochMillis -> PrayerStatus.Completed
                        // Compared by instant, not by prayer: after Isha the next prayer is
                        // tomorrow's Fajr, and matching on the name alone would highlight
                        // today's Fajr — a row that is hours in the past.
                        time.startEpochMillis == next?.startEpochMillis -> PrayerStatus.Next
                        else -> PrayerStatus.Later
                    },
                )
            },
            next = next?.let { time ->
                NextPrayer(
                    prayer = time.prayer,
                    startEpochMillis = time.startEpochMillis,
                    isTomorrow = today != null && time.startEpochMillis > today.times.last().startEpochMillis,
                )
            },
            locationLabel = today?.displayLocation(),
            zoneId = today?.zoneId,
            lastUpdatedEpochMillis = today?.fetchedAtEpochMillis,
            isLoading = isLoading,
            messageRes = messageRes,
            isPermissionRequestable = isPermissionRequestable,
        )
    }

    /**
     * Coordinates when the reverse lookup found no name.
     *
     * Ugly on purpose, and better than the alternatives: a blank row makes the page look
     * broken, and "Unknown location" is less informative than four decimal places a user
     * can at least sanity-check against where they think they are.
     *
     * [Locale.US] rather than the device locale, because coordinates are conventionally
     * written with a decimal point everywhere — "51,5072, -0,1276" would read as four
     * numbers rather than two.
     */
    private fun PrayerDay.displayLocation(): String =
        locationLabel ?: String.format(Locale.US, "%.4f, %.4f", latitude, longitude)

    private fun Throwable.toMessageRes(): Int = when (this) {
        // Checked before the two below: this one is fixed by a grant the app can ask for,
        // and retrying without asking would fail identically forever.
        is LocationPermissionDeniedException -> ResR.string.prayer_error_permission
        is LocationUnavailableException -> ResR.string.prayer_error_no_location
        is IOException -> ResR.string.prayer_error_unreachable
        else -> ResR.string.prayer_error_generic
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /**
         * One second, because the countdown shows seconds in its final hour. Coarser would
         * visibly stutter; finer would be work nobody can see.
         */
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}
