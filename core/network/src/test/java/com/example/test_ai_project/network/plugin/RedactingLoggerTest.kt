package com.example.test_ai_project.network.plugin

import com.example.test_ai_project.network.testing.RecordingLogger
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RedactingLoggerTest {

    @Test
    fun `an appid value is replaced and the rest of the line is untouched`() {
        val written = mutableListOf<String>()
        val logger = RedactingLogger(RecordingLogger(written))

        logger.log("REQUEST: https://api.openweathermap.org/data/2.5/weather?lat=64.1&appid=secret")

        assertThat(written.single()).doesNotContain("secret")
        assertThat(written.single()).contains("lat=64.1")
        assertThat(written.single()).contains("appid=")
    }

    @Test
    fun `an api_key value is replaced`() {
        val written = mutableListOf<String>()
        val logger = RedactingLogger(RecordingLogger(written))

        logger.log("RESPONSE: 200 for https://api.themoviedb.org/3/movie/popular?api_key=secret")

        assertThat(written.single()).doesNotContain("secret")
    }

    /**
     * The parameter is rarely last. Eating to the end of the line would take the rest of the URL
     * with it and make the log useless, which is the failure that would get redaction removed.
     */
    @Test
    fun `redaction stops at the next parameter`() {
        val written = mutableListOf<String>()
        val logger = RedactingLogger(RecordingLogger(written))

        logger.log("GET /weather?appid=secret&units=metric&lang=en HTTP/1.1")

        assertThat(written.single()).doesNotContain("secret")
        assertThat(written.single()).contains("units=metric")
        assertThat(written.single()).contains("lang=en")
    }

    @Test
    fun `both credentials in one line are replaced`() {
        val written = mutableListOf<String>()
        val logger = RedactingLogger(RecordingLogger(written))

        logger.log("?api_key=first&x=1 then ?appid=second&y=2")

        assertThat(written.single()).doesNotContain("first")
        assertThat(written.single()).doesNotContain("second")
        assertThat(written.single()).contains("x=1")
        assertThat(written.single()).contains("y=2")
    }

    /**
     * A body or header line has nothing to redact, and must come through whole — this logger sits
     * in front of everything the client writes, not only request lines.
     */
    @Test
    fun `a line with no credential passes through unchanged`() {
        val written = mutableListOf<String>()
        val logger = RedactingLogger(RecordingLogger(written))
        val line = """BODY START {"page":1,"results":[]}"""

        logger.log(line)

        assertThat(written.single()).isEqualTo(line)
    }
}
