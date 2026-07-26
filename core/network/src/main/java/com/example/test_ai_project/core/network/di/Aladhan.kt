package com.example.test_ai_project.core.network.di

import javax.inject.Qualifier

/**
 * Marks the Retrofit instance that talks to Aladhan.
 *
 * A qualifier for the same reason as [Tmdb], minus the credential: this backend needs no
 * auth, so it deliberately reuses the *unauthenticated* OkHttp client. Naming it makes
 * that reuse explicit — and makes it a compile error for the prayer API to be built on the
 * TMDB client, which would post a bearer token to a third party.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Aladhan
