package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.WeatherRepository
import com.example.test_ai_project.core.model.WeatherSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Observes the cached weather snapshot.
 *
 * Takes no arguments, unlike [GetPrayerScheduleUseCase], and the asymmetry is the point: a
 * prayer schedule is bound to a calendar date, so the query has to be swapped at midnight,
 * whereas there is only ever one cached snapshot and it is always the latest. Nothing about
 * *when* the screen is looking changes which rows it reads — only how the age of those rows is
 * described, which is the UI's job.
 */
class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
) {
    operator fun invoke(): Flow<WeatherSnapshot?> = weatherRepository.observeWeather()
}
