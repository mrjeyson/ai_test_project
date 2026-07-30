package com.example.test_ai_project.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention applied by every `presentation` module. Bundles library + compose + hilt
 * setup and wires the shared design system, which also carries the MVI base.
 *
 * Note which dependency is absent, and stays absent: a feature's `data` module. A
 * presentation module talks to services declared in its own `domain` module and never sees
 * the implementation, so the convention encodes that rule rather than leaving it to
 * reviewer discipline. `:app` is what puts the `data` modules on the runtime classpath.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("testai.android.library")
            apply("testai.android.compose")
            apply("testai.android.hilt")
            // Type-safe navigation routes are @Serializable, so every feature needs this.
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        dependencies {
            add("implementation", project(":core:resource"))

            add("implementation", libs.lib("androidx-core-ktx"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-ktx"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("androidx-hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("kotlinx-serialization-json"))
        }
    }
}
