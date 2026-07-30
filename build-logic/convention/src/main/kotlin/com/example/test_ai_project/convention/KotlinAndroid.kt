package com.example.test_ai_project.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * SDK levels and Java compatibility, applied identically to the application module and
 * every library module.
 *
 * Note: [CommonExtension] is NOT generic in AGP 9 — the `CommonExtension<*, *, *, *, *, *>`
 * signature seen in AGP 8-era samples will not compile here. AGP 9 also exposes
 * `compileSdk`/`compileSdkMinor` as plain `Int` properties, and declares only getters for
 * `defaultConfig`/`compileOptions`/`buildFeatures`, so this configures via property access
 * rather than the block DSL available in build scripts.
 *
 * Kotlin's `jvmTarget` is deliberately not set here: AGP 9 has built-in Kotlin support and
 * derives it from `compileOptions`, so there is no `org.jetbrains.kotlin.android` plugin to
 * configure.
 */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    val javaVersion = JavaVersion.toVersion(libs.version("jvmTarget"))

    extension.compileSdk = libs.intVersion("compileSdk")
    extension.compileSdkMinor = libs.intVersion("compileSdkMinor")
    extension.defaultConfig.minSdk = libs.intVersion("minSdk")
    extension.compileOptions.sourceCompatibility = javaVersion
    extension.compileOptions.targetCompatibility = javaVersion

    dependencies.add("implementation", libs.lib("kotlinx-coroutines-android"))
}

/**
 * The unit-test toolchain every module is entitled to, so a new module can write a test
 * without first editing its build script.
 */
internal fun Project.configureUnitTestDependencies() {
    dependencies.apply {
        add("testImplementation", libs.lib("junit"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("turbine"))
        add("testImplementation", libs.lib("mockk"))
        add("testImplementation", libs.lib("truth"))
    }
}
