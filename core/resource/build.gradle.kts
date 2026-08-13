plugins {
    id("testai.android.library")
    id("testai.android.compose")
    // The design system owns which palette is painted, and that outlives a composition —
    // so the store behind it is an injectable singleton like any other service.
    id("testai.android.hilt")
}

android {
    namespace = "com.example.test_ai_project.resource"
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

    // This module also carries the MVI base, so a presentation module inherits
    // BaseViewModel by depending on the design system alone.
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.kotlinx.coroutines.core)
}
