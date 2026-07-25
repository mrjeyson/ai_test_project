package com.example.test_ai_project.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * A load outcome that a UI layer can render exhaustively.
 *
 * Preferred over Kotlin's [Result] because it also models the in-flight state,
 * and because [Result] cannot be used as a `Flow` type parameter cleanly.
 */
sealed interface DataResult<out T> {
    data object Loading : DataResult<Nothing>
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val throwable: Throwable) : DataResult<Nothing>
}

/**
 * Wraps a cold stream so that upstream failures become [DataResult.Error] values
 * instead of cancelling the collector.
 */
fun <T> Flow<T>.asResult(): Flow<DataResult<T>> = this
    .map<T, DataResult<T>> { DataResult.Success(it) }
    .onStart { emit(DataResult.Loading) }
    .catch { emit(DataResult.Error(it)) }
