package com.example.test_ai_project.core.domain.exception

/**
 * The location permission is not granted.
 *
 * Distinct from [LocationUnavailableException] because the two need different words and
 * offer different actions: this one is fixed by a grant the app can ask for, and retrying
 * without asking would fail identically forever.
 *
 * Declared in the domain layer rather than reusing [SecurityException], for the same reason
 * as [MovieServiceNotConfiguredException]: a feature module should not have to recognise
 * the platform exception the data layer happened to catch.
 */
class LocationPermissionDeniedException(
    override val message: String = "Location permission is not granted",
    override val cause: Throwable? = null,
) : Exception(message, cause)

/**
 * The permission is granted, but the platform has no position to give — location services
 * switched off, or no provider has produced a fix yet.
 *
 * Transient, unlike [LocationPermissionDeniedException]: the same call can succeed a moment
 * later, so retrying is a reasonable thing to offer.
 */
class LocationUnavailableException(
    override val message: String = "No location fix is available",
    override val cause: Throwable? = null,
) : Exception(message, cause)
