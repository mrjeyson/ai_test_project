package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.LocationRepository
import com.example.test_ai_project.core.model.UserLocation
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Observes the cached position fix.
 *
 * Thin by design — there is nothing to decide about a single cached row, and the value of
 * the seam is that the map's ViewModel never learns that Room exists.
 */
class GetLastKnownLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<UserLocation?> = locationRepository.observeLastKnownLocation()
}
