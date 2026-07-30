plugins {
    id("testai.android.library")
}

android {
    namespace = "com.example.test_ai_project.home.domain"
}

dependencies {
    // Flow, on the service contracts the presentation layer collects.
    api(libs.kotlinx.coroutines.core)

    // javax.inject annotations only — no Dagger runtime and no code generation.
    implementation(libs.hilt.core)
}
