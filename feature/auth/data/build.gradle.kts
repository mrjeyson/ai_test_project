plugins {
    id("testai.android.library")
    id("testai.android.hilt")
}

android {
    namespace = "com.example.test_ai_project.auth.data"
}

dependencies {
    implementation(project(":feature:auth:domain"))

    // withContext/Dispatchers, in the on-device sources. Declared here rather than arriving
    // transitively: this module is the one that uses it.
    implementation(libs.kotlinx.coroutines.core)
}
