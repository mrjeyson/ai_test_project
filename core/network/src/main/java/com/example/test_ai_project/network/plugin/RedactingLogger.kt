package com.example.test_ai_project.network.plugin

import android.util.Log
import io.ktor.client.plugins.logging.Logger

/**
 * Where Ktor's log lines actually go on Android.
 *
 * Ktor's own `Logger.DEFAULT` writes through SLF4J, which on Android resolves to a no-op
 * binding — the logs would simply vanish. One `Log.d` is the whole implementation, kept
 * separate from [RedactingLogger] so redaction can be tested without a device.
 */
internal object AndroidLogger : Logger {
    override fun log(message: String) {
        Log.d(TAG, message)
    }

    private const val TAG = "HttpClient"
}

/**
 * Scrubs credentials that travel in the URL before anything is written.
 *
 * Not decoration. The log level is `ALL` in debug builds, so every request line and response
 * URL is written out — and two of this app's three providers authenticate with a query
 * parameter: OpenWeatherMap's `appid` and TMDB's v3 `api_key`.
 *
 * Redaction rather than install ordering, which is what the OkHttp version of this leaned on.
 * Ordering can only ever hide the request leg: the response is logged from the URL that
 * actually went out, credential attached, so a plugin that logged "before" auth would still
 * print the key on the way back. Rewriting the text closes both legs at once, and does not
 * depend on which phase any other plugin happens to install into.
 *
 * The bearer header needs none of this — Ktor's `Logging` plugin has `sanitizeHeader`, which
 * the client configuration points at `Authorization`.
 */
internal class RedactingLogger(
    private val delegate: Logger = AndroidLogger,
) : Logger {

    override fun log(message: String) = delegate.log(redact(message))

    private fun redact(message: String): String =
        SENSITIVE_PARAMETERS.fold(message) { text, pattern ->
            pattern.replace(text) { match -> match.groupValues[1] + REDACTED }
        }

    private companion object {
        const val REDACTED = "██"

        /**
         * Matched on the wire form rather than by parsing the URL, because a log line is
         * prose with a URL somewhere inside it — there is nothing to parse. Each pattern
         * keeps the `?`/`&` and the parameter name, and eats the value up to the next
         * delimiter or whitespace.
         */
        val SENSITIVE_PARAMETERS = listOf("appid", "api_key").map { name ->
            Regex("""([?&]$name=)[^&\s"']*""", RegexOption.IGNORE_CASE)
        }
    }
}
