package com.example.test_ai_project.home.domain.service

import com.example.test_ai_project.home.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

/** The current conditions and forecast for wherever the user is. */
interface WeatherService {

    /** Emits `null` until there has ever been a successful fetch. */
    fun observeWeather(): Flow<WeatherSnapshot?>

    /**
     * Acquires a location if needed, then refetches.
     *
     * @param relocate forces a fresh position fix and a forced refetch.
     * @throws com.example.test_ai_project.home.domain.exception.LocationUnavailableException
     *   when no fix can be obtained.
     */
    suspend fun refresh(relocate: Boolean = false)
}
