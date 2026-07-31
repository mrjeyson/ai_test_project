package com.example.test_ai_project.network.di

import javax.inject.Qualifier

/**
 * Marks the `HttpClient` that talks to OpenWeatherMap.
 *
 * A qualifier for the same reason as [Tmdb]: this backend needs a credential of its own, so
 * it gets a client of its own. Naming it makes it a compile error to build the weather API on
 * the TMDB client — which would send a TMDB bearer token to a third party — or on the
 * unauthenticated one, which would send no key and earn a 401.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenWeather
