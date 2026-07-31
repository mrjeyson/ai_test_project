package com.example.test_ai_project.network.result

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.dto.MoviePageDto
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.MockResponder
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Driven through a real client rather than by throwing the exceptions by hand, because the point
 * of the migration is that the *client's* failures are the ones being normalized — a test that
 * constructed a `ResponseException` itself would still pass if the client stopped raising one.
 */
class SafeApiCallTest {

    @Test
    fun `a body that parses is a success`() = runTest {
        val page = call { respondJson("""{"page": 2, "results": [], "total_pages": 9}""") }

        assertThat(page).isInstanceOf(AppResult.Success::class.java)
        assertThat((page as AppResult.Success).data.totalPages).isEqualTo(9)
    }

    /**
     * The status only becomes a failure because the clients are built with `expectSuccess = true`.
     * Without it Ktor would hand back the error page as an ordinary response and this would be a
     * *success* carrying an empty movie list.
     */
    @Test
    fun `a 401 is Unauthorized`() = runTest {
        val result = call { respondError(HttpStatusCode.Unauthorized) }

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Unauthorized::class.java)
    }

    @Test
    fun `a 403 is Unauthorized`() = runTest {
        val result = call { respondError(HttpStatusCode.Forbidden) }

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Unauthorized::class.java)
    }

    @Test
    fun `a 500 is a Server failure carrying the code`() = runTest {
        val result = call { respondError(HttpStatusCode.InternalServerError) }

        val error = (result as AppResult.Failure).error
        assertThat(error).isEqualTo(AppError.Server(code = 500, message = "Internal Server Error"))
    }

    @Test
    fun `a 404 is a Server failure rather than something else`() = runTest {
        val result = call { respondError(HttpStatusCode.NotFound) }

        assertThat((result as AppResult.Failure).error)
            .isEqualTo(AppError.Server(code = 404, message = "Not Found"))
    }

    @Test
    fun `a transport failure is a Network failure`() = runTest {
        val result = call { throw IOException("airplane mode") }

        assertThat((result as AppResult.Failure).error)
            .isEqualTo(AppError.Network("airplane mode"))
    }

    /**
     * A response the app cannot parse is a server contract problem, not a transport one, so it
     * must not be reported to the user as "check your connection".
     */
    @Test
    fun `a malformed body is Unknown, not Network`() = runTest {
        val result = call { respondJson("""{"page": "not a number"}""") }

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Unknown::class.java)
    }

    /** A 200 of HTML — a captive portal, most often — is unparseable but not a transport error. */
    @Test
    fun `a 200 with an unexpected content type is Unknown`() = runTest {
        val result = call {
            respond(
                content = "<html>sign in to continue</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.Unknown::class.java)
    }

    /**
     * Swallowing this would break structured concurrency: a cancelled coroutine would carry on
     * and report a "failure" for work nobody is waiting for any more.
     */
    @Test
    fun `cancellation is rethrown rather than captured`() = runTest {
        val thrown = runCatching {
            safeApiCall<Unit> { throw CancellationException("scope closed") }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    private suspend fun call(respond: MockResponder): AppResult<MoviePageDto> {
        val backend = MockBackend(respond)
        val client = backend.client(NetworkModule.TMDB_BASE_URL)
        return safeApiCall { client.get("movie/popular").body<MoviePageDto>() }
    }
}
