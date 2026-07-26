plugins {
    id("testaiproject.android.feature")
}

android {
    namespace = "com.example.test_ai_project.feature.faceverification"
}

dependencies {
    // The only module that knows CameraX and ML Kit exist. The domain sees normalised
    // rectangles and angles, never a `Face` or an `ImageProxy`.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.face.detection)

    // ContextCompat.getMainExecutor — CameraX must be bound on the main thread, and
    // Context.getMainExecutor is API 28 while minSdk is 24.
    implementation(libs.androidx.core.ktx)

    // rememberLauncherForActivityResult, for the camera permission request.
    implementation(libs.androidx.activity.compose)
}
