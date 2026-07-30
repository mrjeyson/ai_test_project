package com.example.test_ai_project.convention

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room, including schema export.
 *
 * The schema directory is resolved from `layout.projectDirectory`, the `ProjectLayout`
 * service rather than `Project` itself, so nothing captures `Project` into a task action —
 * which is what keeps this compatible with the configuration cache.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
            apply("androidx.room")
        }

        extensions.configure<RoomExtension> {
            // Checked in, so migrations can be diffed against a known-good baseline.
            //
            // Uses the typed `Directory` overload rather than interpolating the directory
            // into a string: `Directory.toString()` happens to yield an absolute path today,
            // but that is not part of its contract, and a wrong value here would surface as
            // a late, cryptic failure inside room-compiler.
            schemaDirectory(layout.projectDirectory.dir("schemas"))
        }

        dependencies {
            add("api", libs.lib("androidx-room-runtime"))
            add("api", libs.lib("androidx-room-ktx"))
            add("ksp", libs.lib("androidx-room-compiler"))
        }
    }
}
