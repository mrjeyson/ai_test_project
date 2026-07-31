package com.example.test_ai_project.network.plugin

import io.ktor.client.plugins.api.createClientPlugin
import java.io.IOException

/**
 * Raised when no OpenWeatherMap key was compiled in.
 *
 * A distinct type for the same reason as [TmdbNotConfiguredException]: "you are offline, here
 * is the cache" and "this build has no credentials" are indistinguishable from inside a
 * `catch` and call for opposite words on screen. The first fixes itself when the signal comes
 * back; the second never will.
 */
class OpenWeatherNotConfiguredException : IOException(
    "No OpenWeatherMap key. Add `openweather.apiKey=<key>` to local.properties, or set the " +
        "OPENWEATHER_API_KEY environment variable, then rebuild.",
)

/** How [OpenWeatherAuth] is told which key this build has. */
class OpenWeatherAuthConfig {
    var apiKey: String = ""
}

/**
 * Attaches the API key to every request on the OpenWeatherMap client.
 *
 * A query parameter rather than a header, because that is the only scheme the provider
 * accepts — there is no bearer-token equivalent, so unlike [TmdbAuth] there is no choice to
 * make here.
 *
 * That also means the credential is part of the URL, which no header rule can hide. Keeping it
 * out of the log is [RedactingLogger]'s job rather than this plugin's, and deliberately so: a
 * URL credential shows up on the *response* leg too, where install order cannot help.
 *
 * Doing this here rather than as a parameter on each endpoint means the key appears in exactly
 * one place and no new endpoint can be added without it.
 */
val OpenWeatherAuth = createClientPlugin("OpenWeatherAuth", ::OpenWeatherAuthConfig) {
    val apiKey = pluginConfig.apiKey

    onRequest { request, _ ->
        // Fail here rather than letting an unauthenticated request go out. The provider
        // answers 401 "Invalid API key", which sends whoever reads it hunting for a wrong
        // credential instead of an absent one.
        if (apiKey.isBlank()) throw OpenWeatherNotConfiguredException()

        request.url.parameters.append("appid", apiKey)
    }
}
