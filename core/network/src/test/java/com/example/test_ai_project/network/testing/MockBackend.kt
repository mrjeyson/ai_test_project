package com.example.test_ai_project.network.testing

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.plugin.RedactingLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** How a test says what the far end answers with. `MockEngine`'s handler, named. */
internal typealias MockResponder =
    suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

/**
 * A client configured exactly as the app configures one, with `MockEngine` in place of the
 * engine and every request it sends recorded.
 *
 * Built through [NetworkModule.httpClient] on purpose. Content negotiation, `expectSuccess` and
 * the base-URL merge are all properties of that configuration, so a test that assembled its own
 * client would be asserting against a second implementation and would keep passing after the
 * real one changed.
 */
internal class MockBackend(
    private val handler: MockResponder,
) {

    /** Every request that reached the engine, in order. */
    val requests: MutableList<HttpRequestData> = mutableListOf()

    /** Everything the client asked its logger to write. */
    val logLines: MutableList<String> = mutableListOf()

    /** The single request the engine saw — fails the test if there was not exactly one. */
    val request: HttpRequestData get() = requests.single()

    fun client(
        baseUrl: String,
        configure: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient = NetworkModule.httpClient(
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
        json = NetworkModule.providesJson(),
        // The same composition `NetworkModule.providesHttpLogger` builds, with only the sink
        // swapped — a unit test cannot read logcat. Keeping the redaction in place is what lets
        // a test assert that a credential never reaches the log.
        logger = RedactingLogger(RecordingLogger(logLines)),
        baseUrl = baseUrl,
        configure = configure,
    )
}

/** Collects log lines instead of writing them to logcat, which no unit test can read. */
internal class RecordingLogger(
    private val lines: MutableList<String>,
) : Logger {
    override fun log(message: String) {
        lines += message
    }
}

/** A 200 with a JSON body — the shape every endpoint here expects. */
internal fun MockRequestHandleScope.respondJson(body: String): HttpResponseData = respond(
    content = body,
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, "application/json"),
)
