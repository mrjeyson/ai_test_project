package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.exception.LocationUnavailableException
import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.time.DateProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Brings the prayer schedule up to date: find out where the device is, fetch the timetable
 * for there, and re-derive the alerts from it.
 *
 * Three steps in one use case rather than three the ViewModel sequences itself, because
 * they are not independent. Prayer times are meaningless without coordinates, and alerts
 * are meaningless if they disagree with the times on screen — binding them here removes
 * any path where a caller does two of the three.
 *
 * @throws com.example.test_ai_project.core.domain.exception.LocationPermissionDeniedException
 *   if a fix is needed and the permission is not granted.
 * @throws LocationUnavailableException if a fix is needed and the platform has none.
 * @throws java.io.IOException if the device is offline or the request fails.
 */
class RefreshPrayerTimesUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val schedulePrayerAlerts: SchedulePrayerAlertsUseCase,
    private val dateProvider: DateProvider,
) {
    /**
     * @param relocate true when the user explicitly asked to re-detect where they are.
     *   Acquiring a fix costs battery and, on a cold radio, several seconds, so the ordinary
     *   path reuses the cached one and only an explicit request pays for a new one.
     */
    suspend operator fun invoke(relocate: Boolean = false) {
        // A cold start has no fix at all, so it takes the same path as an explicit
        // re-detect: without coordinates there is no timetable to ask for.
        if (relocate || locationRepository.observeLastKnownLocation().first() == null) {
            locationRepository.refreshCurrentLocation()
        }

        // Read after the refresh rather than from its return value: the repository
        // deliberately publishes a new fix only through the cache, so this is the one
        // place a position can arrive from.
        val location = locationRepository.observeLastKnownLocation().first()
            ?: throw LocationUnavailableException()

        prayerTimesRepository.refresh(
            location = location,
            today = dateProvider.today(),
            // A re-detect has to bypass the freshness check. The user is telling us the
            // cached coordinates are wrong, which is exactly the input that check trusts.
            force = relocate,
        )

        // Last, and only on success: alerts are derived from the cache, so they are
        // re-derived once the cache is settled. A failed fetch leaves both the schedule and
        // the alarms as they were, which is the correct offline outcome.
        schedulePrayerAlerts()
    }
}
