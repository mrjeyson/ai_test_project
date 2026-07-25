plugins {
    id("testaiproject.android.library")
    id("testaiproject.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.test_ai_project.core.network"

    // The only module that needs BuildConfig — the android.library convention deliberately
    // leaves buildFeatures.buildConfig alone so this opt-in works.
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
