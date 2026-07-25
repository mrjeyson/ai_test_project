plugins {
    id("testaiproject.android.feature")
}

android {
    namespace = "com.example.test_ai_project.feature.home"
}

// Compose, Hilt, serialization, navigation, lifecycle, and the permitted core layers
// (:core:ui, :core:domain, :core:model, :core:common) all come from the feature
// convention. Notably absent, and enforced there: :core:data.
