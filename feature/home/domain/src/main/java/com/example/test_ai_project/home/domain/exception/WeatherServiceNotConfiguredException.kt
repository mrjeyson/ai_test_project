package com.example.test_ai_project.home.domain.exception

/**
 * The weather provider has no key configured in this build.
 *
 * Exists for the same reason as [MovieServiceNotConfiguredException], and is a separate type
 * rather than a shared "not configured" one because the message names the property to set and
 * the two are different properties. Collapsing them would mean a screen telling the user to
 * add a TMDB key to fix the weather.
 *
 * Declared in the domain layer rather than reusing the provider's own exception, because a
 * feature module cannot see `:core:network` and should not learn to.
 */
class WeatherServiceNotConfiguredException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
