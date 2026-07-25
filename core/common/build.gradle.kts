plugins {
    id("testaiproject.android.library")
    id("testaiproject.android.hilt")
}

android {
    namespace = "com.example.test_ai_project.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
