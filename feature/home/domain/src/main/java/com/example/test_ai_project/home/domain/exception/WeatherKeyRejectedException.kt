package com.example.test_ai_project.home.domain.exception

/**
 * The weather provider refused the key this build was compiled with.
 *
 * Distinct from [WeatherServiceNotConfiguredException] because the remedy is different, and
 * telling the user the wrong one wastes their afternoon. "No key was configured" is fixed by
 * adding one; this is what you get when a key *is* present and the provider says no — most often
 * because it is newly issued and not yet active, which OpenWeatherMap can take a couple of hours
 * to do. Advising someone to add a key they can plainly see in their own `local.properties` reads
 * as the app being broken.
 *
 * Also distinct from a plain network failure: the request arrived and was answered, so there is
 * nothing to retry until something changes at the provider's end.
 */
class WeatherKeyRejectedException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
