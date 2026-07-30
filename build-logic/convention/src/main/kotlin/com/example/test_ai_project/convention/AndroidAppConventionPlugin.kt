package com.example.test_ai_project.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The application module. Shares SDK/Java configuration with libraries and adds the one
 * setting only an application has: `targetSdk`.
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = libs.intVersion("targetSdk")
        }
        configureUnitTestDependencies()
    }
}
