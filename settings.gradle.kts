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

// core — shared, feature-agnostic infrastructure
include(":core:resource")
include(":core:network")
include(":core:database")

// features — one domain/data/presentation trio per feature
include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")
include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")
