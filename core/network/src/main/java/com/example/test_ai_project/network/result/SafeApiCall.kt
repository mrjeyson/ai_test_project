package com.example.test_ai_project.network.result

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Runs a suspending network call and normalizes everything it can throw into an
 * [AppResult].
 *
 * This is the single place in the app where an HTTP exception is turned into a value.
 * Every remote data source wraps its calls in it, so no `try`/`catch` around the HTTP client
 * appears anywhere else.
 *
 * [CancellationException] is deliberately rethrown rather than captured: swallowing it
 * would break structured concurrency, leaving a cancelled coroutine to carry on and report
 * a "failure" for work nobody is waiting for any more.
 *
 * [ResponseException] is Ktor's answer to a non-2xx, and it only exists because the clients set
 * `expectSuccess = true` — left off, a 500 would arrive here as a *successful* attempt to parse
 * an error page. `ResponseException` does not descend from [IOException], so the two branches
 * below stay independent and the ordering between them carries no meaning.
 */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (response: ResponseException) {
    val status = response.response.status
    AppResult.Failure(
        when (status.value) {
            401, 403 -> AppError.Unauthorized(status.description)
            else -> AppError.Server(code = status.value, message = status.description)
        },
    )
} catch (io: IOException) {
    AppResult.Failure(AppError.Network(io.message))
} catch (convert: ContentConvertException) {
    // A response the app cannot parse is a server contract problem, not a transport one,
    // so it must not be reported to the user as "check your connection". Ktor wraps the
    // underlying kotlinx failure in this, so it is the case that actually fires in practice.
    AppResult.Failure(AppError.Unknown(convert.message))
} catch (serialization: SerializationException) {
    // Still caught, for a body deserialized outside content negotiation.
    AppResult.Failure(AppError.Unknown(serialization.message))
} catch (noTransformation: NoTransformationFoundException) {
    // A 2xx whose Content-Type nothing is registered for — an HTML captive-portal page
    // answering 200, most often. Not a transport failure either, and not parseable.
    AppResult.Failure(AppError.Unknown(noTransformation.message))
}
