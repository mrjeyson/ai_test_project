plugins {
    id("testaiproject.android.application.compose")
    id("testaiproject.android.hilt")
}

android {
    namespace = "com.example.test_ai_project"

    defaultConfig {
        applicationId = "com.example.test_ai_project"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    // The app module is pure assembly: it wires features into a nav graph and owns the
    // Hilt root. :core:data is present only so its @Module bindings land in the
    // SingletonComponent — no app code imports from it.
    implementation(project(":feature:splash"))
    implementation(project(":feature:login"))
    implementation(project(":feature:faceverification"))
    implementation(project(":feature:home"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    // Material 3 and the Compose UI artifacts arrive transitively via :core:ui's `api`
    // exports; the Compose BOM comes from the compose convention.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    // Backports the Android 12 splash screen window to API 24, so the launch window
    // matches the splash route on every supported device.
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
