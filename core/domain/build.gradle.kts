plugins {
    id("testaiproject.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)

    // javax.inject annotations only — no Dagger runtime, no code generation. This module
    // must not take the Hilt convention, which would drag KSP into a pure-Kotlin layer.
    implementation(libs.hilt.core)
}
