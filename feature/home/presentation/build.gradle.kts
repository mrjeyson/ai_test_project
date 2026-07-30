import java.util.Properties

plugins {
    id("testai.android.feature")
}

/**
 * Google Maps SDK key.
 *
 * Sourced from `local.properties` (git-ignored) or an environment variable for CI, never
 * from a checked-in file.
 *
 * Absent resolves to empty rather than failing the build, so the project still compiles and
 * its tests still run on a machine with no key. The map then renders as an empty grid with
 * an "Authorization failure" line in logcat — a keyless build is a broken map, not a broken
 * build.
 *
 * Substituted into this module's manifest rather than the app's, so the key travels with
 * the feature that needs it, exactly as the camera permission travels with
 * `:feature:faceverification:presentation`.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val mapsApiKey: String = localProperties.getProperty("maps.apiKey")
    ?: providers.environmentVariable("MAPS_API_KEY").orNull
    ?: ""

android {
    namespace = "com.example.test_ai_project.home.presentation"

    defaultConfig {
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
}

dependencies {
    implementation(project(":feature:home:domain"))

    // The map itself. Note what is *not* here: play-services-location. Acquiring a fix is
    // data-layer work behind LocationService, and this module only ever renders the result.
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
}
