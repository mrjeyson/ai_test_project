plugins {
    id("testaiproject.android.library.compose")
}

android {
    namespace = "com.example.test_ai_project.core.ui"
}

dependencies {
    // `api`, not `implementation`: this module IS the design system, so consumers get the
    // Material 3 and Compose UI surface transitively rather than re-declaring it.
    // The BOM and the debug-only tooling dependency come from the compose convention.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)

    // Also `api`: remote images are a design-system concern, and the alternative is every
    // feature that shows one re-declaring Coil and drifting on version.
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)
}
