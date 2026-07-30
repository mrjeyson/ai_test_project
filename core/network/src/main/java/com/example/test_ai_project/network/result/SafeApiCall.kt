package com.example.test_ai_project.network.result

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Runs a suspending network call and normalizes everything it can throw into an
 * [AppResult].
 *
 * This is the single place in the app where an HTTP exception is turned into a value.
 * Every remote data source wraps its calls in it, so no `try`/`catch` around Retrofit
 * appears anywhere else.
 *
 * [CancellationException] is deliberately rethrown rather than captured: swallowing it
 * would break structured concurrency, leaving a cancelled coroutine to carry on and report
 * a "failure" for work nobody is waiting for any more.
 */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (http: HttpException) {
    AppResult.Failure(
        when (http.code()) {
            401, 403 -> AppError.Unauthorized(http.message())
            else -> AppError.Server(code = http.code(), message = http.message())
        },
    )
} catch (io: IOException) {
    AppResult.Failure(AppError.Network(io.message))
} catch (serialization: SerializationException) {
    // A response the app cannot parse is a server contract problem, not a transport one,
    // so it must not be reported to the user as "check your connection".
    AppResult.Failure(AppError.Unknown(serialization.message))
}
