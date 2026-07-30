plugins {
    id("testai.android.feature")
}

android {
    namespace = "com.example.test_ai_project.auth.presentation"
}

// Compose, Hilt, serialization, navigation, lifecycle and :core:resource all
// come from the feature convention. Notably absent, and enforced there: :feature:auth:data.
dependencies {
    implementation(project(":feature:auth:domain"))

    // The only module that knows CameraX and ML Kit exist. The domain sees normalised
    // rectangles and angles, never a `Face` or an `ImageProxy`.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.face.detection)
}
