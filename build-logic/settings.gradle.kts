// `build-logic` is a separate, included build. It does not inherit the root build's
// repository configuration, so it declares its own.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Reuse the project's single source of version truth rather than duplicating
    // versions here — the convention plugins read the same catalog the modules do.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
