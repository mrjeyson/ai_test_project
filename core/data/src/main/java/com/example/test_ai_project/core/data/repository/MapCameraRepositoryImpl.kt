package com.example.test_ai_project.core.data.repository

import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.data.mapper.toDomain
import com.example.test_ai_project.core.data.mapper.toEntity
import com.example.test_ai_project.core.database.dao.LocationDao
import com.example.test_ai_project.core.domain.repository.MapCameraRepository
import com.example.test_ai_project.core.model.MapCamera
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class MapCameraRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MapCameraRepository {

    override suspend fun lastCamera(): MapCamera? = withContext(ioDispatcher) {
        locationDao.mapCamera()?.toDomain()
    }

    override suspend fun saveCamera(camera: MapCamera) = withContext(ioDispatcher) {
        locationDao.upsertMapCamera(camera.toEntity())
    }
}
