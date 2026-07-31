package com.example.test_ai_project.network.di

import javax.inject.Qualifier

/**
 * Marks the `HttpClient` that talks to Aladhan.
 *
 * A qualifier for the same reason as [Tmdb], minus the credential: this backend needs no
 * auth, so its client has no auth plugin installed at all. Naming it makes that absence
 * explicit — and makes it a compile error for the prayer API to be built on the TMDB client,
 * which would post a bearer token to a third party.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Aladhan
