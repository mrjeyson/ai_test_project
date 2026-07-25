package com.example.test_ai_project.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Java and Kotlin targets for the pure-Kotlin modules (`:core:model`, `:core:domain`).
 *
 * Deliberately uses the running JDK and only sets the bytecode target, rather than
 * declaring `jvmToolchain(..)` — a toolchain would make Gradle download a second JDK.
 */
internal fun Project.configureKotlinJvm() {
    val target = libs.version("jvmTarget")

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.toVersion(target)
        targetCompatibility = JavaVersion.toVersion(target)
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(target))
    }
}
