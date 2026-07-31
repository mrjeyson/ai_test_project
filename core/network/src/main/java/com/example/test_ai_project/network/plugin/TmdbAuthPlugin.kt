package com.example.test_ai_project.network.plugin

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.io.IOException

/**
 * Raised when no TMDB credential was compiled in.
 *
 * A distinct type, and not just an [IOException] with a message, because the difference
 * matters to the user: "you are offline, here is the cache" and "this build has no
 * credentials" look identical from inside a `catch` but call for opposite reactions.
 *
 * [IOException] rather than a bare exception because that is what it *is* to every layer
 * above: a request that never left the device. It also keeps the type inside the same catch
 * surface `safeApiCall` already covers, so an unrecognised build-configuration failure
 * degrades to "network unavailable" instead of escaping as a crash.
 */
class TmdbNotConfiguredException : IOException(
    "No TMDB credential. Add `tmdb.apiKey=<v3 API key>` or " +
        "`tmdb.accessToken=<v4 read access token>` to local.properties, or set the " +
        "TMDB_API_KEY / TMDB_ACCESS_TOKEN environment variable, then rebuild.",
)

/** How [TmdbAuth] is told which credentials this build has. */
class TmdbAuthConfig {
    var accessToken: String = ""
    var apiKey: String = ""
}

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
 * A client plugin rather than a parameter on each endpoint means the credential appears in
 * exactly one place and no new endpoint can be added without it. Installing it on the TMDB
 * client alone — see the `@Tmdb` qualifier — is what stops the bearer token being posted to
 * another provider.
 */
val TmdbAuth = createClientPlugin("TmdbAuth", ::TmdbAuthConfig) {
    val accessToken = pluginConfig.accessToken
    val apiKey = pluginConfig.apiKey

    onRequest { request, _ ->
        when {
            accessToken.isNotBlank() ->
                request.header(HttpHeaders.Authorization, "Bearer $accessToken")

            apiKey.isNotBlank() -> request.url.parameters.append("api_key", apiKey)

            // Fail here rather than letting an unauthenticated request go out. TMDB would
            // answer 401 "Invalid API key", which sends whoever reads it hunting for a
            // wrong credential instead of an absent one.
            else -> throw TmdbNotConfiguredException()
        }
    }
}
