package com.example.test_ai_project.core.domain.exception

/**
 * The movie provider has no credential configured in this build.
 *
 * Exists so the UI can tell a setup fault apart from a network fault. Both surface as a
 * failed fetch, but "you are offline, here is the cache" and "this build was compiled
 * without an API key" need different words on screen — the first resolves itself when the
 * signal returns, and the second never will.
 *
 * Declared in the domain layer rather than reusing the provider's own exception type,
 * because a feature module cannot see `:core:network` and should not learn to.
 */
class MovieServiceNotConfiguredException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
