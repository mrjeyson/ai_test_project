package com.example.test_ai_project.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Opt-in Hilt wiring. Applied per module rather than folded into the library convention,
 * because the `domain` modules hold nothing but interfaces and models and need neither the
 * Dagger runtime nor code generation.
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("com.google.dagger.hilt.android")
        }

        dependencies {
            add("implementation", libs.lib("hilt-android"))
            add("ksp", libs.lib("hilt-android-compiler"))
        }
    }
}
