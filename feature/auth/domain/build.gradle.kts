plugins {
    id("testai.android.library")
}

android {
    namespace = "com.example.test_ai_project.auth.domain"
}

dependencies {
    // javax.inject annotations only — no Dagger runtime and no code generation. A domain
    // module holds contracts and models, so it must not take the Hilt convention.
    implementation(libs.hilt.core)
}
