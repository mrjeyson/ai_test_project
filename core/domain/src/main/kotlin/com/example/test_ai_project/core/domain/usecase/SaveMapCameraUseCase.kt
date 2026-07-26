package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.MapCameraRepository
import com.example.test_ai_project.core.model.MapCamera
import javax.inject.Inject

/** Records where the map came to rest, so the next visit opens there. */
class SaveMapCameraUseCase @Inject constructor(
    private val mapCameraRepository: MapCameraRepository,
) {
    suspend operator fun invoke(camera: MapCamera) = mapCameraRepository.saveCamera(camera)
}
