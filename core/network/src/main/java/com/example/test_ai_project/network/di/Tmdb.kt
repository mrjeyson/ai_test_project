package com.example.test_ai_project.network.di

import javax.inject.Qualifier

/**
 * Marks the OkHttp client and Retrofit instance that talk to TMDB.
 *
 * The app has two backends with nothing in common, and the distinction has to be
 * enforceable: the TMDB client carries a bearer token, and attaching that token to
 * requests aimed at any other host would leak the credential. A qualifier makes the wrong
 * wiring a compile error rather than a code-review catch.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Tmdb
