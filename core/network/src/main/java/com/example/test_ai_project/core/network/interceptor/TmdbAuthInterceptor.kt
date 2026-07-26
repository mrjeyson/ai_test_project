package com.example.test_ai_project.core.network.interceptor

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Raised when no TMDB credential was compiled in.
 *
 * A distinct type, and not just an [IOException] with a message, because the difference
 * matters to the user: "you are offline, here is the cache" and "this build has no
 * credentials" look identical from inside a `catch` but call for opposite reactions.
 *
 * It still extends [IOException] — an OkHttp interceptor may only throw that.
 */
class TmdbNotConfiguredException : IOException(
    "No TMDB credential. Add `tmdb.apiKey=<v3 API key>` or " +
        "`tmdb.accessToken=<v4 read access token>` to local.properties, or set the " +
        "TMDB_API_KEY / TMDB_ACCESS_TOKEN environment variable, then rebuild.",
)

/**
 * Authenticates every request on the TMDB client.
 *
 * TMDB hands out two credentials on the same settings page and they are not
 * interchangeable: the v4 "API Read Access Token" is a JWT sent as a bearer header, while
 * the v3 "API Key" is a 32-character string sent as an `api_key` query parameter. Sending
 * either one the other's way earns a 401 that reads as if the credential were invalid.
 *
 * Rather than making the reader pick correctly, this accepts both and routes each the way
 * TMDB expects. The token wins if somehow both are present, being the newer scheme.
 *
 * Doing this here rather than as `@Header`/`@Query` parameters means the credential
 * appears in exactly one place and no new endpoint can be added without it.
 */
internal class TmdbAuthInterceptor(
    private val accessToken: String,
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val authenticated = when {
            accessToken.isNotBlank() -> request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")

            apiKey.isNotBlank() -> request.newBuilder()
                .url(
                    request.url.newBuilder()
                        .addQueryParameter("api_key", apiKey)
                        .build(),
                )

            // Fail here rather than letting an unauthenticated request go out. TMDB would
            // answer 401 "Invalid API key", which sends whoever reads it hunting for a
            // wrong credential instead of an absent one.
            else -> throw TmdbNotConfiguredException()
        }

        return chain.proceed(authenticated.addHeader("Accept", "application/json").build())
    }
}
