package com.example.test_ai_project.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds Compose support to any Android module — application or library. Applied alongside
 * the app/library convention rather than folded into it, so non-UI modules
 * (`:core:network`, every `domain` and `data` module) never pay for the Compose compiler.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val common: CommonExtension = extensions.findByType(ApplicationExtension::class.java)
            ?: extensions.findByType(LibraryExtension::class.java)
            ?: error(
                "testai.android.compose requires the android application or library " +
                    "convention to be applied first.",
            )

        common.buildFeatures.compose = true

        dependencies {
            val bom = libs.lib("androidx-compose-bom")
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))

            add("implementation", libs.lib("androidx-compose-ui"))
            add("implementation", libs.lib("androidx-compose-ui-graphics"))
            add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
            add("implementation", libs.lib("androidx-compose-material3"))
            add("implementation", libs.lib("androidx-activity-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))

            add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
            add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))

            add("androidTestImplementation", libs.lib("androidx-compose-ui-test-junit4"))
        }
    }
}
