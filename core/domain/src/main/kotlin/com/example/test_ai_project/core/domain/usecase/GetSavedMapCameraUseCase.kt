package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.MapCameraRepository
import com.example.test_ai_project.core.model.MapCamera
import javax.inject.Inject

/** Reads the viewport the map was last left on, or null if it never has been. */
class GetSavedMapCameraUseCase @Inject constructor(
    private val mapCameraRepository: MapCameraRepository,
) {
    suspend operator fun invoke(): MapCamera? = mapCameraRepository.lastCamera()
}
