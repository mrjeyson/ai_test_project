package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * The weather, as the rest of the app sees it.
 *
 * The same offline-first split as [PrayerTimesRepository] and [LocationRepository]: reading is
 * local and always succeeds, fetching is separate, and a caller that ignores a fetch failure
 * still has a complete screen.
 *
 * The reason the split is drawn here differs, though, and it is worth being honest about.
 * Cached prayer times stay *correct* indefinitely — the timetable for tomorrow is the same
 * whether it was fetched today or last week. Cached weather goes stale within the hour and
 * only stays *useful*: it is the last thing known about the sky, not the current state of it.
 * So the snapshot carries its own timestamp and the screen says how old it is, rather than
 * presenting an eight-hour-old temperature as the present one.
 */
interface WeatherRepository {

    /**
     * The cached snapshot, re-emitted whenever it is replaced.
     *
     * Never throws and never completes. Emits `null` until a fetch has ever succeeded, which
     * on a first run with no network is until one does.
     */
    fun observeWeather(): Flow<WeatherSnapshot?>

    /**
     * Fetches current conditions and the forecast for [location], and replaces the cache with
     * both.
     *
     * Skips the network when the cache is recent enough and close enough to [location] to say
     * the same thing, unless [force] is set — which is what an explicit refresh passes.
     *
     * Both requests must succeed for either to be written. Half a snapshot — a new temperature
     * beside a forecast for the previous city — is worse than an old whole one.
     *
     * @throws com.example.test_ai_project.core.domain.exception.WeatherServiceNotConfiguredException
     *   if this build has no API key.
     * @throws java.io.IOException if the device is offline or the request fails.
     */
    suspend fun refresh(location: UserLocation, force: Boolean = false)
}
