import java.util.Properties

plugins {
    id("testaiproject.android.library")
    id("testaiproject.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

/**
 * TMDB credentials.
 *
 * Both are read because TMDB issues both on the same settings page and users reach for
 * whichever they see first: `tmdb.accessToken` is the v4 read access token (a JWT) and
 * `tmdb.apiKey` is the v3 API key. The interceptor accepts either.
 *
 * Sourced from `local.properties` (git-ignored) or an environment variable for CI, and
 * never from a checked-in file — a credential committed to history is one that has to be
 * rotated.
 *
 * Absent resolves to empty rather than failing the build, so the project still compiles
 * and its tests still run on a machine with no credential. The interceptor turns the empty
 * value into one clear, named error at request time instead of an opaque 401.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val tmdbAccessToken: String = localProperties.getProperty("tmdb.accessToken")
    ?: providers.environmentVariable("TMDB_ACCESS_TOKEN").orNull
    ?: ""

val tmdbApiKey: String = localProperties.getProperty("tmdb.apiKey")
    ?: providers.environmentVariable("TMDB_API_KEY").orNull
    ?: ""

/**
 * OpenWeatherMap key, read under the same rule as the TMDB credentials above.
 *
 * Only one to read, unlike TMDB: the provider issues a single key and accepts it only as an
 * `appid` query parameter, so there is no second scheme to support.
 *
 * Absent resolves to empty rather than failing the build, so the project still compiles and
 * its tests still run on a machine with no key. The interceptor turns the empty value into one
 * clear, named error at request time — and the Weather tab shows whatever it last cached,
 * which is the behaviour that tab is built around.
 */
val openWeatherApiKey: String = localProperties.getProperty("openweather.apiKey")
    ?: providers.environmentVariable("OPENWEATHER_API_KEY").orNull
    ?: ""

android {
    namespace = "com.example.test_ai_project.core.network"

    // The only module that needs BuildConfig — the android.library convention deliberately
    // leaves buildFeatures.buildConfig alone so this opt-in works.
    defaultConfig {
        buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"$tmdbAccessToken\"")
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openWeatherApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "LOG_HTTP_BODIES", "true")
        }
        release {
            buildConfigField("boolean", "LOG_HTTP_BODIES", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
}
