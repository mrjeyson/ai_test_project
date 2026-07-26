package com.example.test_ai_project.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.test_ai_project.core.database.entity.LastKnownLocationEntity
import com.example.test_ai_project.core.database.entity.MapCameraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    /** Null until the first fix is ever written. Emits again on every later fix. */
    @Query("SELECT * FROM last_known_location WHERE id = ${LastKnownLocationEntity.SINGLETON_ID}")
    fun observeLastKnownLocation(): Flow<LastKnownLocationEntity?>

    @Upsert
    suspend fun upsertLastKnownLocation(location: LastKnownLocationEntity)

    /**
     * A one-shot read, not a [Flow], because the camera is restored once when the screen
     * opens. Observing it would feed the map's own saved position back into the map.
     */
    @Query("SELECT * FROM map_camera WHERE id = ${MapCameraEntity.SINGLETON_ID}")
    suspend fun mapCamera(): MapCameraEntity?

    @Upsert
    suspend fun upsertMapCamera(camera: MapCameraEntity)
}
