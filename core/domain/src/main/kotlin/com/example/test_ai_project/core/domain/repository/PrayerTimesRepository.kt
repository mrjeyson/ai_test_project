package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.PrayerSchedule
import com.example.test_ai_project.core.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * The prayer timetable, as the rest of the app sees it.
 *
 * The same offline-first split as [MovieRepository] and [LocationRepository], and it earns
 * its place here more than anywhere else in the app: a prayer time is something the user
 * needs at a fixed moment, in a place they may have no signal. Reading is local and always
 * succeeds; fetching is separate, and a caller that ignores its failure still has a
 * complete, correct day on screen.
 */
interface PrayerTimesRepository {

    /**
     * The cached schedule starting at [today], re-emitted whenever the cache changes.
     *
     * Never throws and never completes. Emits `null` until a day has ever been cached,
     * which on a first run with no network is until there is one.
     */
    fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?>

    /**
     * The same window, read once — for the alarm and boot receivers, which have no UI to
     * keep up to date and no reason to hold a subscription open.
     */
    suspend fun schedule(today: CalendarDate): PrayerSchedule?

    /**
     * Fetches [today] and the day after for [location], and writes both to the cache.
     *
     * Two days, not one, because the schedule has to answer "what is next" after the last
     * prayer of the evening — and because the alarms set overnight are tomorrow's.
     *
     * Skips the network when both days are already cached for somewhere close enough to
     * [location] to give the same times, unless [force] is set — which is what an explicit
     * re-detect passes.
     *
     * @throws java.io.IOException if the device is offline or the request fails.
     */
    suspend fun refresh(location: UserLocation, today: CalendarDate, force: Boolean = false)
}
