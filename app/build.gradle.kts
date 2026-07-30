plugins {
    id("testai.android.app")
    id("testai.android.compose")
    id("testai.android.hilt")
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
    // The app module is pure assembly: it wires screens into a nav graph and owns the
    // Hilt root.
    //
    // Note the split. A `presentation` module is an `implementation` dependency because
    // the nav graph calls into it; its matching `data` module is `runtimeOnly`, so its
    // service implementations join the Hilt graph at run time and are invisible at compile
    // time. That is what actually stops a screen reaching past its own service contracts —
    // the rule is enforced by the classpath, not by review.
    implementation(project(":core:resource"))

    implementation(project(":feature:auth:presentation"))
    runtimeOnly(project(":feature:auth:data"))
    implementation(project(":feature:home:presentation"))
    runtimeOnly(project(":feature:home:data"))

    // Material 3 and the Compose UI artifacts arrive transitively via :core:resource's `api`
    // exports; the Compose BOM comes from the compose convention.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    // Backports the Android 12 splash screen window to API 24, so the branded launch
    // window looks the same on every supported device.
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
