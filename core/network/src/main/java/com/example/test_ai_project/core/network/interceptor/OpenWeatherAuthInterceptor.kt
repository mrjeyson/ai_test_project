package com.example.test_ai_project.core.network.interceptor

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Raised when no OpenWeatherMap key was compiled in.
 *
 * A distinct type for the same reason as [TmdbNotConfiguredException]: "you are offline, here
 * is the cache" and "this build has no credentials" are indistinguishable from inside a
 * `catch` and call for opposite words on screen. The first fixes itself when the signal comes
 * back; the second never will.
 *
 * It still extends [IOException] — an OkHttp interceptor may only throw that.
 */
class OpenWeatherNotConfiguredException : IOException(
    "No OpenWeatherMap key. Add `openweather.apiKey=<key>` to local.properties, or set the " +
        "OPENWEATHER_API_KEY environment variable, then rebuild.",
)

/**
 * Attaches the API key to every request on the OpenWeatherMap client.
 *
 * A query parameter rather than a header, because that is the only scheme the provider
 * accepts — there is no bearer-token equivalent, so unlike the TMDB interceptor there is no
 * choice to make here.
 *
 * That also means the credential is part of the URL, which `redactHeader` cannot hide. It is
 * why the logging interceptor is installed *before* this one on the client: an application
 * interceptor logs the request as it reaches it, so logging never sees the key at all.
 *
 * Doing this here rather than as a `@Query` parameter on each endpoint means the key appears
 * in exactly one place and no new endpoint can be added without it.
 */
internal class OpenWeatherAuthInterceptor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Fail here rather than letting an unauthenticated request go out. The provider
        // answers 401 "Invalid API key", which sends whoever reads it hunting for a wrong
        // credential instead of an absent one.
        if (apiKey.isBlank()) throw OpenWeatherNotConfiguredException()

        val request = chain.request()
        val authenticated = request.newBuilder()
            .url(
                request.url.newBuilder()
                    .addQueryParameter("appid", apiKey)
                    .build(),
            )
            .addHeader("Accept", "application/json")
            .build()

        return chain.proceed(authenticated)
    }
}
