package com.example.test_ai_project.core.data.repository

import android.location.Location
import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.data.location.GeocoderPlaceNameSource
import com.example.test_ai_project.core.data.mapper.nextDay
import com.example.test_ai_project.core.data.mapper.toAladhanPath
import com.example.test_ai_project.core.data.mapper.toDomain
import com.example.test_ai_project.core.data.mapper.toEntity
import com.example.test_ai_project.core.data.mapper.toIsoString
import com.example.test_ai_project.core.database.dao.PrayerTimesDao
import com.example.test_ai_project.core.database.entity.PrayerDayEntity
import com.example.test_ai_project.core.domain.repository.PrayerTimesRepository
import com.example.test_ai_project.core.domain.time.TimeProvider
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.PrayerDay
import com.example.test_ai_project.core.model.PrayerSchedule
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.core.network.api.AladhanApi
import com.example.test_ai_project.core.network.dto.PrayerTimingsDataDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class PrayerTimesRepositoryImpl @Inject constructor(
    private val prayerTimesDao: PrayerTimesDao,
    private val aladhanApi: AladhanApi,
    private val placeNameSource: GeocoderPlaceNameSource,
    private val timeProvider: TimeProvider,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : PrayerTimesRepository {

    override fun observeSchedule(today: CalendarDate): Flow<PrayerSchedule?> =
        prayerTimesDao.observeFrom(today.toIsoString()).map { days -> days.toSchedule(today) }

    override suspend fun schedule(today: CalendarDate): PrayerSchedule? =
        withContext(ioDispatcher) {
            prayerTimesDao.daysFrom(today.toIsoString()).toSchedule(today)
        }

    override suspend fun refresh(
        location: UserLocation,
        today: CalendarDate,
        force: Boolean,
    ) = withContext(ioDispatcher) {
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
