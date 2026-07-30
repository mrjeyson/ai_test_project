package com.example.test_ai_project.home.domain.service

import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.PrayerSchedule
import kotlinx.coroutines.flow.Flow

/**
 * The daily prayer schedule, and the alarms that follow from it.
 *
 * Scheduling alerts is part of this service rather than a separate one because the two are
 * inseparable in practice: every refresh has to re-arm the alarms, or the user gets alerts
 * for yesterday's times.
 */
interface PrayerService {

    fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?>

    /**
     * Acquires a location if needed, refetches the schedule, and re-arms the alerts.
     *
     * @param relocate forces a fresh position fix and a forced refetch, for the explicit
     *   "use my current location" action rather than the automatic open-the-tab load.
     * @throws com.example.test_ai_project.home.domain.exception.LocationUnavailableException
     *   when no fix can be obtained, because a prayer schedule without a location is not a
     *   degraded answer — it is the wrong one.
     */
    suspend fun refresh(relocate: Boolean = false)

    /**
     * Re-arms the alarms from whatever schedule is already cached.
     *
     * Called on boot, where there is no network and no UI — only the cache and the
     * alarm manager.
     */
    suspend fun scheduleAlerts()
}
