plugins {
    `kotlin-dsl`
}

group = "com.example.test_ai_project.buildlogic"

dependencies {
    // compileOnly: the convention plugins compile against these DSLs, but the plugin
    // JARs reach subprojects via the root build's `plugins { alias(..) apply false }`
    // block. See the comment in the root build.gradle.kts.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApp") {
            id = "testai.android.app"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidAppConventionPlugin"
        }
        register("androidLibrary") {
            id = "testai.android.library"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "testai.android.feature"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "testai.android.compose"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "testai.android.hilt"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "testai.android.room"
            implementationClass =
                "com.example.test_ai_project.convention.AndroidRoomConventionPlugin"
        }
    }
}
