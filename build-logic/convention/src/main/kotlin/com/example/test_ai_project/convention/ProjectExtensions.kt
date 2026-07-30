package com.example.test_ai_project.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * The `libs` version catalog, as seen from a convention plugin.
 *
 * Generated `libs.*` accessors only exist inside build scripts, so plugin code has to
 * look the catalog up by name. `build-logic/settings.gradle.kts` points this at the
 * project's own `gradle/libs.versions.toml`, so there is exactly one version source.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("Library alias '$alias' not found in libs.versions.toml")
    }

internal fun VersionCatalog.version(name: String): String =
    findVersion(name).orElseThrow {
        IllegalStateException("Version '$name' is missing from libs.versions.toml")
    }.requiredVersion

internal fun VersionCatalog.intVersion(name: String): Int =
    version(name).toIntOrNull()
        ?: error("Version '$name' in libs.versions.toml must be an integer")
