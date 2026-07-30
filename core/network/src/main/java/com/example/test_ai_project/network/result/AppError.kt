package com.example.test_ai_project.network.result

/**
 * What went wrong, in terms the domain layer can act on.
 *
 * The point of this type is that it is the *last* place an HTTP concept appears. A `data`
 * module maps an [AppError] onto its own feature's error before returning it, so a service
 * contract never leaks `IOException`, `HttpException`, or a status code to the screen that
 * has to render it.
 *
 * Each case carries an optional server-supplied [message]. When the backend explains
 * itself, that explanation is better than any string the app could invent; when it does
 * not, the feature falls back to a localized message of its own.
 */
sealed interface AppError {

    val message: String?

    /** No usable connection: DNS failure, timeout, airplane mode. Retrying may work. */
    data class Network(override val message: String? = null) : AppError

    /** The request reached the server and it answered with a failure [code]. */
    data class Server(val code: Int, override val message: String? = null) : AppError

    /** Credentials are missing, expired or rejected — a 401 or 403. */
    data class Unauthorized(override val message: String? = null) : AppError

    /** Anything not worth distinguishing, including serialization failures. */
    data class Unknown(override val message: String? = null) : AppError
}
