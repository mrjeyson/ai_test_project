package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.exception.LocationUnavailableException
import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Brings the weather up to date: find out where the device is, then fetch conditions for there.
 *
 * Two steps in one use case rather than two the ViewModel sequences itself, for the reason
 * [RefreshPrayerTimesUseCase] binds its three: they are not independent. A temperature without
 * coordinates is a temperature for nowhere, and there is no useful path where a caller does one
 * and not the other.
 *
 * One step shorter than the prayer equivalent, because there is nothing to derive afterwards.
 * Prayer times schedule alarms and so have to be re-derived on every successful fetch; weather
 * is only ever displayed.
 *
 * @throws com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
 *   if a fix is needed and the permission is not granted.
 * @throws LocationUnavailableException if a fix is needed and the platform has none.
 * @throws com.example.test_ai_project.core.domain.exception.WeatherServiceNotConfiguredException
 *   if this build has no API key.
 * @throws java.io.IOException if the device is offline or the request fails.
 */
class RefreshWeatherUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
) {
    /**
     * @param relocate true when the user explicitly asked to re-detect where they are.
     *   Acquiring a fix costs battery and, on a cold radio, several seconds, so the ordinary
     *   path reuses the cached one and only an explicit request pays for a new one.
     */
    suspend operator fun invoke(relocate: Boolean = false) {
        // A cold start has no fix at all, so it takes the same path as an explicit re-detect:
        // without coordinates there is nothing to ask the weather service about.
        if (relocate || locationRepository.observeLastKnownLocation().first() == null) {
            locationRepository.refreshCurrentLocation()
        }

        // Read after the refresh rather than from its return value: the repository deliberately
        // publishes a new fix only through the cache, so this is the one place a position can
        // arrive from.
        val location = locationRepository.observeLastKnownLocation().first()
            ?: throw LocationUnavailableException()

        weatherRepository.refresh(
            location = location,
            // A re-detect has to bypass the freshness check. The user is telling us the cached
            // snapshot is wrong, which is exactly the input that check trusts.
            force = relocate,
        )
    }
}
