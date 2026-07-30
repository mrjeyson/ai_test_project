package com.example.test_ai_project.home.data.service

import android.location.Location
import com.example.test_ai_project.home.data.local.GeocoderPlaceNameSource
import com.example.test_ai_project.home.data.mapper.nextDay
import com.example.test_ai_project.home.data.mapper.toAladhanPath
import com.example.test_ai_project.home.data.mapper.toDomain
import com.example.test_ai_project.home.data.mapper.toEntity
import com.example.test_ai_project.home.data.mapper.toIsoString
import com.example.test_ai_project.database.dao.PrayerTimesDao
import com.example.test_ai_project.database.entity.PrayerDayEntity
import com.example.test_ai_project.home.domain.exception.LocationUnavailableException
import com.example.test_ai_project.home.domain.service.LocationService
import com.example.test_ai_project.home.domain.service.PrayerAlarmScheduler
import com.example.test_ai_project.home.domain.service.PrayerService
import com.example.test_ai_project.home.domain.service.DateProvider
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.PrayerDay
import com.example.test_ai_project.home.domain.model.PrayerSchedule
import com.example.test_ai_project.home.domain.model.UserLocation
import com.example.test_ai_project.network.api.AladhanApi
import com.example.test_ai_project.network.dto.PrayerTimingsDataDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class DefaultPrayerService @Inject constructor(
    private val prayerTimesDao: PrayerTimesDao,
    private val aladhanApi: AladhanApi,
    private val placeNameSource: GeocoderPlaceNameSource,
    private val locationService: LocationService,
    private val prayerAlarmScheduler: PrayerAlarmScheduler,
    private val dateProvider: DateProvider,
    private val timeProvider: TimeProvider,
) : PrayerService {

    override fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?> =
        prayerTimesDao.observeFrom(today.toIsoString()).map { days -> days.toSchedule(today) }

    /**
     * Acquire a fix if needed, refetch, then re-arm the alarms.
     *
     * The last step is not optional and is easy to forget from a caller: without it a
     * refresh leaves yesterday's alarms pending, so the user is alerted at the wrong times
     * by a schedule that is visibly correct on screen.
     */
    override suspend fun refresh(relocate: Boolean) {
        if (relocate || locationService.observeLastKnownLocation().first() == null) {
            locationService.refreshCurrentLocation()
        }
        val location = locationService.observeLastKnownLocation().first()
            ?: throw LocationUnavailableException()

        refreshFor(location = location, today = dateProvider.today(), force = relocate)
        scheduleAlerts()
    }

    /**
     * Only a bounded window of alarms is ever pending, so each fired alert has to roll the
     * window forward. That is what makes the chain self-perpetuating with no background
     * service — see `PrayerAlarmReceiver`.
     */
    override suspend fun scheduleAlerts() {
        val schedule = cachedSchedule(dateProvider.today())
        prayerAlarmScheduler.replaceAlerts(
            schedule
                ?.upcomingAfter(timeProvider.nowEpochMillis())
                .orEmpty()
                .take(PrayerAlarmScheduler.MAX_PENDING_ALERTS),
        )
    }

    private suspend fun cachedSchedule(today: CalendarDate): PrayerSchedule? =
        withContext(Dispatchers.IO) {
            prayerTimesDao.daysFrom(today.toIsoString()).toSchedule(today)
        }

    private suspend fun refreshFor(
        location: UserLocation,
        today: CalendarDate,
        force: Boolean,
    ) = withContext(Dispatchers.IO) {
        val tomorrow = today.nextDay()
        if (!force && isCached(today, location) && isCached(tomorrow, location)) {
            return@withContext
        }

        val todayTimings = fetch(today, location)
        val tomorrowTimings = fetch(tomorrow, location)


        val locationLabel = placeNameSource.placeName(location.latitude, location.longitude)
        val fetchedAt = timeProvider.nowEpochMillis()

        prayerTimesDao.upsertDays(
            listOf(
                todayTimings.toEntity(today, location, locationLabel, fetchedAt),
                tomorrowTimings.toEntity(tomorrow, location, locationLabel, fetchedAt),
            ),
        )


        prayerTimesDao.deleteBefore(today.toIsoString())
    }


    private suspend fun fetch(date: CalendarDate, location: UserLocation): PrayerTimingsDataDto {
        val response = aladhanApi.getTimings(
            date = date.toAladhanPath(),
            latitude = location.latitude,
            longitude = location.longitude,
        )
        if (response.code != HTTP_OK) {
            // IOException rather than a bespoke type: to every caller above this, "the
            // provider would not answer" and "the network would not carry it" are the same
            // situation with the same response — keep showing the cache.
            throw IOException("Aladhan returned ${response.code}: ${response.status}")
        }
        return response.data
    }


    private suspend fun isCached(date: CalendarDate, location: UserLocation): Boolean {
        val cached = prayerTimesDao.day(date.toIsoString()) ?: return false
        return cached.distanceTo(location) < RELOCATION_THRESHOLD_METERS
    }

    private fun PrayerDayEntity.distanceTo(location: UserLocation): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            latitude,
            longitude,
            location.latitude,
            location.longitude,
            results,
        )
        return results[0]
    }

    private fun PrayerTimingsDataDto.toEntity(
        date: CalendarDate,
        location: UserLocation,
        locationLabel: String?,
        fetchedAtEpochMillis: Long,
    ) = toEntity(
        date = date,
        latitude = location.latitude,
        longitude = location.longitude,
        locationLabel = locationLabel,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )
    private fun List<PrayerDayEntity>.toSchedule(today: CalendarDate): PrayerSchedule? {
        val todayIso = today.toIsoString()
        val tomorrowIso = today.nextDay().toIsoString()

        val todayDay = firstOrNull { it.date == todayIso }?.toDomain() ?: return null
        val tomorrowDay: PrayerDay? = firstOrNull { it.date == tomorrowIso }?.toDomain()

        return PrayerSchedule(today = todayDay, tomorrow = tomorrowDay)
    }

    private companion object {
        const val HTTP_OK = 200
        const val RELOCATION_THRESHOLD_METERS = 10_000f
    }
}
