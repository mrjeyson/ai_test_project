plugins {
    id("testaiproject.android.library")
    id("testaiproject.android.hilt")
}

android {
    namespace = "com.example.test_ai_project.core.data"
}

dependencies {
    // Implements the interfaces declared in :core:domain and is the ONLY module that
    // knows both Room entities and network DTOs exist.
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // The fused location provider. Confined to this module by the same rule that confines
    // Retrofit here: acquiring a fix is data-layer work, and :feature:home only renders
    // the cached result.
    implementation(libs.play.services.location)
}
