package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * Acquires a fresh fix and caches it.
 *
 * Throws on failure rather than swallowing it — the caller has cached content on screen
 * either way, and needs to know whether to say why it is not moving.
 */
class RefreshCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke() = locationRepository.refreshCurrentLocation()
}
