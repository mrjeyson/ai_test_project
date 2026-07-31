package com.example.test_ai_project.network.plugin

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * TMDB accepts two credentials by two different schemes and answers an identical 401 when
 * either is sent the wrong way, so which one goes where is not a detail that can be left to a
 * reading of the plugin.
 */
class TmdbAuthPluginTest {

    @Test
    fun `a v4 access token travels as a bearer header`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.TMDB_BASE_URL) {
            install(TmdbAuth) { accessToken = "jwt-token" }
        }

        client.get("movie/popular")

        assertThat(backend.request.headers[HttpHeaders.Authorization])
            .isEqualTo("Bearer jwt-token")
        assertThat(backend.request.url.parameters["api_key"]).isNull()
    }

    @Test
    fun `a v3 api key travels as a query parameter`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.TMDB_BASE_URL) {
            install(TmdbAuth) { apiKey = "v3-key" }
        }

        client.get("movie/popular")

        assertThat(backend.request.url.parameters["api_key"]).isEqualTo("v3-key")
        assertThat(backend.request.headers[HttpHeaders.Authorization]).isNull()
    }

    /** The newer scheme, when a build somehow has both. */
    @Test
    fun `the access token wins when both are configured`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.TMDB_BASE_URL) {
            install(TmdbAuth) {
                accessToken = "jwt-token"
                apiKey = "v3-key"
            }
        }

        client.get("movie/popular")

        assertThat(backend.request.headers[HttpHeaders.Authorization])
            .isEqualTo("Bearer jwt-token")
        assertThat(backend.request.url.parameters["api_key"]).isNull()
    }

    /**
     * The named failure is the whole point: an unauthenticated request would come back as a 401
     * that reads as a *wrong* credential, sending whoever debugs it after a key that was never
     * there. Nothing must reach the engine.
     */
    @Test
    fun `no credential fails the request before it is sent`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.TMDB_BASE_URL) {
            install(TmdbAuth) {}
        }

        val failure = runCatching { client.get("movie/popular") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(TmdbNotConfiguredException::class.java)
        assertThat(backend.requests).isEmpty()
    }

    /**
     * The parameter is added to the request the endpoint built, not to a URL that replaces it.
     * A plugin that rebuilt the URL would be a quiet way to lose the base path.
     */
    @Test
    fun `the credential is added without disturbing the endpoint's own parameters`() = runTest {
        val backend = MockBackend { respondJson("{}") }
        val client = backend.client(NetworkModule.TMDB_BASE_URL) {
            install(TmdbAuth) { apiKey = "v3-key" }
        }

        client.get("movie/popular?page=7")

        assertThat(backend.request.url.encodedPath).isEqualTo("/3/movie/popular")
        assertThat(backend.request.url.parameters["page"]).isEqualTo("7")
        assertThat(backend.request.url.parameters["api_key"]).isEqualTo("v3-key")
    }
}
