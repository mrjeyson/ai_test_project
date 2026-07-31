package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OpenWeatherApiTest {

    @Test
    fun `current conditions ask for metric units at the given point`() = runTest {
        val backend = MockBackend { respondJson(CURRENT) }
        val api = OpenWeatherApi(backend.client(NetworkModule.OPEN_WEATHER_BASE_URL))

        api.getCurrentWeather(latitude = 64.14, longitude = -21.94, language = "en")

        val url = backend.request.url
        assertThat(url.encodedPath).isEqualTo("/data/2.5/weather")
        assertThat(url.parameters["lat"]).isEqualTo("64.14")
        assertThat(url.parameters["lon"]).isEqualTo("-21.94")
        assertThat(url.parameters["lang"]).isEqualTo("en")
        // Dropping this would leave the provider answering in Kelvin, which renders as "271°".
        assertThat(url.parameters["units"]).isEqualTo(OpenWeatherApi.UNITS_METRIC)
    }

    @Test
    fun `the forecast is a second endpoint on the same client`() = runTest {
        val backend = MockBackend { respondJson(FORECAST) }
        val api = OpenWeatherApi(backend.client(NetworkModule.OPEN_WEATHER_BASE_URL))

        api.getForecast(latitude = 64.14, longitude = -21.94, language = "en")

        assertThat(backend.request.url.encodedPath).isEqualTo("/data/2.5/forecast")
    }

    @Test
    fun `current conditions deserialize the fields the mapper reads`() = runTest {
        val backend = MockBackend { respondJson(CURRENT) }
        val api = OpenWeatherApi(backend.client(NetworkModule.OPEN_WEATHER_BASE_URL))

        val current = api.getCurrentWeather(latitude = 64.14, longitude = -21.94, language = "en")

        assertThat(current.name).isEqualTo("Reykjavík")
        assertThat(current.timezone).isEqualTo(0)
        assertThat(current.main.temp).isEqualTo(11.2)
        assertThat(current.weather.single().id).isEqualTo(803)
        assertThat(current.sys?.country).isEqualTo("IS")
    }

    /**
     * `wind` and `visibility` are absent here, and the DTOs model them as nullable for exactly
     * this reason — the provider omits what it has not measured.
     */
    @Test
    fun `an omitted optional block leaves the snapshot usable`() = runTest {
        val backend = MockBackend { respondJson(CURRENT) }
        val api = OpenWeatherApi(backend.client(NetworkModule.OPEN_WEATHER_BASE_URL))

        val current = api.getCurrentWeather(latitude = 64.14, longitude = -21.94, language = "en")

        assertThat(current.wind).isNull()
        assertThat(current.visibility).isNull()
    }

    private companion object {
        const val CURRENT = """
            {
              "weather": [{"id": 803, "main": "Clouds", "description": "broken clouds",
                           "icon": "04d"}],
              "main": {"temp": 11.2, "feels_like": 9.4, "humidity": 71},
              "timezone": 0,
              "name": "Reykjavík",
              "sys": {"country": "IS"}
            }
        """

        const val FORECAST = """
            {
              "list": [
                {
                  "dt": 1785500000,
                  "main": {"temp": 10.1, "feels_like": 8.0, "humidity": 80},
                  "weather": [{"id": 500, "main": "Rain", "description": "light rain",
                               "icon": "10n"}]
                }
              ],
              "city": {"name": "Reykjavík", "country": "IS", "timezone": 0}
            }
        """
    }
}
