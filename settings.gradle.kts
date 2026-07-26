pluginManagement {
    // Convention plugins live here. Included from pluginManagement so module scripts can
    // reference them by id in a `plugins { }` block.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "test_ai_project"

include(":app")

// Core — shared, feature-agnostic layers
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:ui")

// Features — one self-contained vertical slice per screen
include(":feature:login")
include(":feature:faceverification")
include(":feature:home")
