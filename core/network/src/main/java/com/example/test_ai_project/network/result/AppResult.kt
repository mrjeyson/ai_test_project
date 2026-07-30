package com.example.test_ai_project.network.result

/**
 * The outcome of a single call into the network layer.
 *
 * Preferred over Kotlin's [Result] because the failure side is a closed set of
 * [AppError]s rather than an open `Throwable`, so a `when` over it is exhaustive and the
 * compiler catches a new error case at every call site.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/**
 * Folds both sides into one value — the shape a `data` service uses to turn an
 * [AppResult] into its own feature's domain result.
 */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}
