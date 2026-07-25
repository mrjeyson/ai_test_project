package com.example.test_ai_project.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * SDK levels and Java compatibility, applied identically to the application module and
 * every library module. Previously duplicated across all 7 Android modules.
 *
 * Note: [CommonExtension] is NOT generic in AGP 9 — the `CommonExtension<*, *, *, *, *, *>`
 * signature seen in AGP 8-era samples will not compile here. AGP 9 also exposes
 * `compileSdk`/`compileSdkMinor` as plain `Int` properties, and declares only getters for
 * `defaultConfig`/`compileOptions`/`buildFeatures`, so this configures via property access
 * rather than the block DSL available in build scripts.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    val javaVersion = JavaVersion.toVersion(libs.version("jvmTarget"))

    extension.compileSdk = libs.intVersion("compileSdk")
    extension.compileSdkMinor = libs.intVersion("compileSdkMinor")
    extension.defaultConfig.minSdk = libs.intVersion("minSdk")
    extension.compileOptions.sourceCompatibility = javaVersion
    extension.compileOptions.targetCompatibility = javaVersion
}

/**
 * Turns a module into a Compose module: enables the build feature, applies the Compose
 * compiler plugin, and puts the Compose BOM on the classpath so no module pins Compose
 * artifact versions itself.
 *
 * Individual Compose artifacts stay in the module scripts, because the configuration
 * differs by intent — `:core:ui` re-exports them with `api`, `:app` consumes them.
 */
internal fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true

    dependencies.apply {
        add("implementation", platform(libs.library("androidx-compose-bom")))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    }
}

/**
 * The unit-test toolchain every module is entitled to, so a new module can write a test
 * without first editing its build script.
 */
internal fun Project.configureUnitTestDependencies() {
    dependencies.apply {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
    }
}
