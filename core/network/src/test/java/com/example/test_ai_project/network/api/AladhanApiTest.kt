package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AladhanApiTest {

    @Test
    fun `the date becomes a path segment and the calculation defaults are sent`() = runTest {
        val backend = MockBackend { respondJson(TIMINGS) }
        val api = AladhanApi(backend.client(NetworkModule.ALADHAN_BASE_URL))

        api.getTimings(date = "30-07-2026", latitude = 41.31, longitude = 69.24)

        val url = backend.request.url
        assertThat(url.encodedPath).isEqualTo("/v1/timings/30-07-2026")
        assertThat(url.parameters["latitude"]).isEqualTo("41.31")
        assertThat(url.parameters["longitude"]).isEqualTo("69.24")
        // Named constants on the wire, not bare numbers. Omitting either lets the provider pick
        // an authority from the coordinates, which is what the defaults exist to prevent.
        assertThat(url.parameters["method"])
            .isEqualTo(AladhanApi.METHOD_MUSLIM_WORLD_LEAGUE.toString())
        assertThat(url.parameters["school"]).isEqualTo(AladhanApi.SCHOOL_HANAFI.toString())
    }

    /**
     * The date is provider-formatted text on a path, so it is appended as a segment rather than
     * spliced into a string. A separator that reached the URL raw would silently address a
     * different endpoint; encoded, it is at worst a date the provider rejects.
     */
    @Test
    fun `a date containing a separator is encoded rather than reshaping the path`() = runTest {
        val backend = MockBackend { respondJson(TIMINGS) }
        val api = AladhanApi(backend.client(NetworkModule.ALADHAN_BASE_URL))

        api.getTimings(date = "30-07-2026/../calendar", latitude = 0.0, longitude = 0.0)

        assertThat(backend.request.url.encodedPath).doesNotContain("calendar/")
        assertThat(backend.request.url.encodedPath).startsWith("/v1/timings/")
    }

    @Test
    fun `the envelope code and the location timezone both survive deserialization`() = runTest {
        val backend = MockBackend { respondJson(TIMINGS) }
        val api = AladhanApi(backend.client(NetworkModule.ALADHAN_BASE_URL))

        val response = api.getTimings(date = "30-07-2026", latitude = 41.31, longitude = 69.24)

        // The provider reports its own failures inside a 200, so `code` is the field the caller
        // checks — losing it would turn a provider error into a plausible-looking timetable.
        assertThat(response.code).isEqualTo(200)
        assertThat(response.data.timings.fajr).isEqualTo("03:41")
        assertThat(response.data.meta.timezone).isEqualTo("Asia/Tashkent")
    }

    private companion object {
        const val TIMINGS = """
            {
              "code": 200,
              "status": "OK",
              "data": {
                "timings": {
                  "Fajr": "03:41",
                  "Sunrise": "05:12",
                  "Dhuhr": "12:37",
                  "Asr": "17:32",
                  "Maghrib": "20:01",
                  "Isha": "21:23"
                },
                "meta": {"timezone": "Asia/Tashkent"}
              }
            }
        """
    }
}
