package com.example.test_ai_project.network.plugin

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.get
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OpenWeatherAuthPluginTest {

    @Test
    fun `the key is attached as appid to every request on the client`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.OPEN_WEATHER_BASE_URL) {
            install(OpenWeatherAuth) { apiKey = "weather-key" }
        }

        client.get("data/2.5/weather")
        client.get("data/2.5/forecast")

        assertThat(backend.requests.map { it.url.parameters["appid"] })
            .containsExactly("weather-key", "weather-key")
    }

    @Test
    fun `an absent key fails the request before it is sent`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.OPEN_WEATHER_BASE_URL) {
            install(OpenWeatherAuth) {}
        }

        val failure = runCatching { client.get("data/2.5/weather") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(OpenWeatherNotConfiguredException::class.java)
        assertThat(backend.requests).isEmpty()
    }

    /**
     * The provider authenticates by query parameter, so the credential ends up in the URL and
     * the log line for the request carries it. Redaction is what keeps it out — asserted here
     * against the real client wiring rather than only against [RedactingLogger] in isolation,
     * because the property that matters is "no build of this client logs the key", not "the
     * regex works".
     */
    @Test
    fun `the key never reaches the log`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.OPEN_WEATHER_BASE_URL) {
            install(OpenWeatherAuth) { apiKey = "weather-key" }
        }

        client.get("data/2.5/weather")

        assertThat(backend.logLines).isNotEmpty()
        assertThat(backend.logLines.none { line -> line.contains("weather-key") }).isTrue()
    }
}
